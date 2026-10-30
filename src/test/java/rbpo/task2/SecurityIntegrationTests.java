package rbpo.task2;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import rbpo.task2.model.*;
import rbpo.task2.repository.*;
import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired VehicleRepository vehicles;
    @Autowired ParkingZoneRepository zones;
    @Autowired ParkingSessionRepository sessions;
    @Autowired ParkingPassRepository passes;
    @Autowired PasswordEncoder encoder;
    User alice, bob, admin;
    Vehicle car, other;
    ParkingZone zone;
    final String password = "ParkingTest42!";

    @BeforeEach void fixtures() {
        alice = account("alice@test.example", "USER");
        bob = account("bob@test.example", "USER");
        admin = account("admin@test.example", "ADMIN");
        car = vehicle("TEST-A", alice); other = vehicle("TEST-B", bob);
        zone = new ParkingZone(); zone.setCapacity(1); zone.setHourlyPrice(100.0); zones.saveAndFlush(zone);
    }
    User account(String email, String role) {
        User u = new User(); u.setName(email); u.setEmail(email); u.setRole(role);
        u.setPasswordHash(encoder.encode(password)); return users.saveAndFlush(u);
    }
    Vehicle vehicle(String number, User owner) {
        Vehicle v = new Vehicle(); v.setNumber(number); v.setOwner(owner); return vehicles.saveAndFlush(v);
    }
    String registration(String email, String value) throws Exception {
        return json.writeValueAsString(Map.of("name", "Новый пользователь", "email", email, "password", value));
    }
    ParkingSession session(Vehicle v) {
        ParkingSession s = new ParkingSession(); s.setVehicle(v); s.setZone(zone); s.setEntryTime(LocalDateTime.now());
        return sessions.saveAndFlush(s);
    }
    ParkingPass pass(Vehicle v) {
        ParkingPass p = new ParkingPass(); p.setVehicle(v); p.setZone(zone); p.setStatus("ACTIVE");
        p.setValidFrom(LocalDateTime.now()); p.setValidTo(LocalDateTime.now().plusDays(10)); return passes.saveAndFlush(p);
    }

    @Test void anonymousAndWrongPasswordAre401() throws Exception {
        mvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized()).andExpect(header().exists("WWW-Authenticate"));
        mvc.perform(get("/api/vehicles").with(httpBasic(alice.getEmail(), "wrong")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(post("/api/vehicles").with(csrf()).param("ownerId", alice.getId().toString())
                .contentType("application/json").content("{\"number\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }
    @Test void registrationHashesPasswordAndAlwaysCreatesUser() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content(registration("New@Test.Example", password)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
        User u = users.findByEmail("new@test.example").orElseThrow();
        assertThat(u.getPasswordHash()).startsWith("$2").isNotEqualTo(password);
        assertThat(encoder.matches(password, u.getPasswordHash())).isTrue();
        mvc.perform(get("/api/users/" + u.getId()).with(httpBasic("NEW@test.example", password))).andExpect(status().isOk());
    }
    @Test void weakDuplicateAndOverlongPasswordsAreRejected() throws Exception {
        for (String weak : new String[]{"1234567890", "lowercase42!", "UPPERCASE42!", "NoDigitsHere!", "NoSpecialChar42", "A1!" + "я".repeat(40)}) {
            mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                    .content(registration("new@test.example", weak))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content(registration("ALICE@test.example", password))).andExpect(status().isConflict());
    }
    @Test void roleAndStateInjectionRejected() throws Exception {
        for (String field : new String[]{"role", "enabled", "locked", "id", "passwordHash"}) {
            ObjectNode node = (ObjectNode) json.readTree(registration("new@test.example", password));
            node.put(field, "ADMIN");
            mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(node.toString()))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/users/" + alice.getId()).with(httpBasic(alice.getEmail(), password)).with(csrf())
                .contentType("application/json").content("{\"name\":\"Alice\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        assertThat(users.findById(alice.getId()).orElseThrow().getRole()).isEqualTo("USER");
    }
    @Test void csrfRequiredEvenForRegistrationAndAdmin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content(registration("new@test.example", password)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mvc.perform(post("/api/parking-zones").with(httpBasic(admin.getEmail(), password))
                .contentType("application/json").content("{\"capacity\":2,\"hourlyPrice\":10}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/vehicles/"+car.getId()).with(httpBasic(alice.getEmail(), password)).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
    }
    @Test void realCsrfTokenNeedsMatchingSessionAndBasicEveryTime() throws Exception {
        var result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = json.readTree(result.getResponse().getContentAsString());
        var session = (MockHttpSession) result.getRequest().getSession(false);
        String header = token.get("headerName").asText(), value = token.get("token").asText();
        mvc.perform(post("/api/auth/register").session(session).header(header, value).contentType("application/json")
                .content(registration("csrf@test.example", password))).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").session(new MockHttpSession()).header(header, value).contentType("application/json")
                .content(registration("csrf2@test.example", password))).andExpect(status().isForbidden());
        mvc.perform(get("/api/users").session(session).with(httpBasic(alice.getEmail(), password))).andExpect(status().isOk());
        mvc.perform(get("/api/users").session(session)).andExpect(status().isUnauthorized());
        alice.setLocked(true); users.saveAndFlush(alice);
        mvc.perform(get("/api/users").session(session).with(httpBasic(alice.getEmail(), password))).andExpect(status().isUnauthorized());
    }
    @Test void blockedAndDisabledAccountsHaveSameSafeError() throws Exception {
        alice.setEnabled(false); bob.setLocked(true); users.flush();
        String disabled = mvc.perform(get("/api/users").with(httpBasic(alice.getEmail(), password)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String locked = mvc.perform(get("/api/users").with(httpBasic(bob.getEmail(), password)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(disabled).isEqualTo(locked).doesNotContain("password", "hash", "locked", "disabled");
    }
    @Test void listsAndIndividualReadsDoNotExposeOtherOwners() throws Exception {
        var ownSession=session(car); var otherSession=session(other); var ownPass=pass(car); var otherPass=pass(other);
        for (String path : new String[]{"users", "vehicles", "parking-sessions", "parking-passes"}) {
            mvc.perform(get("/api/"+path).with(httpBasic(alice.getEmail(), password)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))));
        }
        for (String path : new String[]{"users/"+bob.getId(), "vehicles/"+other.getId(), "parking-sessions/"+otherSession.getId(), "parking-passes/"+otherPass.getId()}) {
            mvc.perform(get("/api/"+path).with(httpBasic(alice.getEmail(), password))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/users").with(httpBasic(admin.getEmail(), password))).andExpect(jsonPath("$.length()").value(3));
    }
    @Test void ownerCannotCreateOrModifyOtherCars() throws Exception {
        mvc.perform(post("/api/vehicles").param("ownerId",bob.getId().toString()).with(httpBasic(alice.getEmail(),password)).with(csrf())
                .contentType("application/json").content("{\"number\":\"NEW\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/vehicles/"+other.getId()).with(httpBasic(alice.getEmail(),password)).with(csrf())
                .contentType("application/json").content("{\"number\":\"NEW\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/vehicles/"+other.getId()).with(httpBasic(alice.getEmail(),password)).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void ordinaryUserCannotManageZonesUsersOrStates() throws Exception {
        mvc.perform(post("/api/parking-zones").with(httpBasic(alice.getEmail(),password)).with(csrf())
                .contentType("application/json").content("{\"capacity\":2,\"hourlyPrice\":10}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/parking-zones/"+zone.getId()).with(httpBasic(alice.getEmail(),password)).with(csrf())
                .contentType("application/json").content("{\"capacity\":2}")).andExpect(status().isForbidden());
        for (String path : new String[]{"users/"+alice.getId(), "parking-zones/"+zone.getId()})
            mvc.perform(delete("/api/"+path).with(httpBasic(alice.getEmail(),password)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/users").with(httpBasic(alice.getEmail(),password)).with(csrf()).contentType("application/json")
                .content(registration("extra@test.example",password))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/users/"+alice.getId()+"/state").with(httpBasic(alice.getEmail(),password)).with(csrf())
                .contentType("application/json").content("{\"enabled\":true,\"locked\":false}")).andExpect(status().isForbidden());
    }
    @Test void adminCanManageZonesAndBlockUserButNotSelf() throws Exception {
        mvc.perform(post("/api/parking-zones").with(httpBasic(admin.getEmail(),password)).with(csrf())
                .contentType("application/json").content("{\"capacity\":2,\"hourlyPrice\":10}")).andExpect(status().isCreated());
        mvc.perform(patch("/api/users/"+alice.getId()+"/state").with(httpBasic(admin.getEmail(),password)).with(csrf())
                .contentType("application/json").content("{\"enabled\":true,\"locked\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/users").with(httpBasic(alice.getEmail(),password))).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/users/"+admin.getId()+"/state").with(httpBasic(admin.getEmail(),password)).with(csrf())
                .contentType("application/json").content("{\"enabled\":false,\"locked\":true}")).andExpect(status().isForbidden());
    }
    @Test void businessOperationsCheckOwnership() throws Exception {
        var s=session(other); var p=pass(other);
        for(String path:new String[]{"parking-sessions/entry?vehicleId="+other.getId()+"&zoneId="+zone.getId(),
                "parking-sessions/"+s.getId()+"/exit", "parking-passes/purchase?vehicleId="+other.getId()+"&zoneId="+zone.getId()+"&days=5",
                "parking-passes/"+p.getId()+"/extend?days=5", "vehicles/"+other.getId()+"/transfer?newOwnerId="+alice.getId()}) {
            mvc.perform(post("/api/"+path).with(httpBasic(alice.getEmail(),password)).with(csrf())).andExpect(status().isForbidden());
        }
        for(String path:new String[]{"parking-sessions/"+s.getId(),"parking-passes/"+p.getId()})
            mvc.perform(delete("/api/"+path).with(httpBasic(bob.getEmail(),password)).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void transferRevokesOldOwnerAndCancelsPasses() throws Exception {
        var p=pass(car);
        mvc.perform(post("/api/vehicles/"+car.getId()+"/transfer").param("newOwnerId",bob.getId().toString())
                .with(httpBasic(alice.getEmail(),password)).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/vehicles/"+car.getId()).with(httpBasic(alice.getEmail(),password))).andExpect(status().isForbidden());
        mvc.perform(get("/api/vehicles/"+car.getId()).with(httpBasic(bob.getEmail(),password))).andExpect(status().isOk());
        assertThat(passes.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("CANCELLED");
    }
    @Test void businessRestrictionsStillApplyToAdmin() throws Exception {
        var s=session(car);
        for(String path:new String[]{"vehicles/"+car.getId()+"/transfer?newOwnerId="+bob.getId(),
                "parking-sessions/entry?vehicleId="+car.getId()+"&zoneId="+zone.getId(),
                "parking-sessions/entry?vehicleId="+other.getId()+"&zoneId="+zone.getId()}) {
            mvc.perform(post("/api/"+path).with(httpBasic(admin.getEmail(),password)).with(csrf()))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
        }
        mvc.perform(post("/api/parking-sessions/"+s.getId()+"/exit").with(httpBasic(alice.getEmail(),password)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(100.0));
        mvc.perform(post("/api/parking-sessions/"+s.getId()+"/exit").with(httpBasic(alice.getEmail(),password)).with(csrf()))
                .andExpect(status().isConflict());
    }
    @Test void passesPurchaseExtendAndFreeExitWork() throws Exception {
        String url="/api/parking-passes/purchase?vehicleId="+car.getId()+"&zoneId="+zone.getId()+"&days=10";
        var result=mvc.perform(post(url).with(httpBasic(alice.getEmail(),password)).with(csrf())).andExpect(status().isCreated()).andReturn();
        long id=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post(url).with(httpBasic(alice.getEmail(),password)).with(csrf())).andExpect(status().isConflict());
        mvc.perform(post("/api/parking-passes/"+id+"/extend?days=5").with(httpBasic(alice.getEmail(),password)).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/parking-passes/"+id+"/extend?days=0").with(httpBasic(alice.getEmail(),password)).with(csrf())).andExpect(status().isBadRequest());
        var s=session(car);
        mvc.perform(post("/api/parking-sessions/"+s.getId()+"/exit").with(httpBasic(alice.getEmail(),password)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(0.0));
    }
}
