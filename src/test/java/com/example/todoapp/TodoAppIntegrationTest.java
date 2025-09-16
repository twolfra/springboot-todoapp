package com.example.todoapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TodoAppIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    // ------------------------------------------------------------------------
    // 🔐 AUTH TESTS
    // ------------------------------------------------------------------------

    @Test
    void registerAndLoginShouldWork() throws Exception {
        String username = "bob" + System.currentTimeMillis();
        register(username, "secret");
        Cookie jwtCookie = login(username, "secret");

        // cookie should not be null
        assert jwtCookie != null;
    }

    @Test
    void logoutShouldClearCookie() throws Exception {
        String username = "alice" + System.currentTimeMillis();
        register(username, "secret");
        Cookie jwtCookie = login(username, "secret");

        MvcResult logoutResult = mockMvc.perform(post("/auth/logout")
                        .with(csrf())
                        .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"))
                .andReturn();

        Cookie clearedCookie = logoutResult.getResponse().getCookie("JWT");
        assert clearedCookie != null;
        assert clearedCookie.getValue().isEmpty();
        assert clearedCookie.getMaxAge() == 0;
    }

    @Test
    void invalidLoginShouldReturnUnauthorized() throws Exception {
        String username = "mallory" + System.currentTimeMillis();
        register(username, "secret");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    // ------------------------------------------------------------------------
    // ✅ TASK TESTS (BASIC)
    // ------------------------------------------------------------------------

    @Test
    void userCanCreateAndFetchTasks() throws Exception {
        String username = "charlie" + System.currentTimeMillis();
        register(username, "secret");
        Cookie jwtCookie = login(username, "secret");

        // create task
        mockMvc.perform(post("/tasks")
                        .with(csrf())
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Buy milk\",\"done\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.owner").value(username));

        // fetch tasks
        mockMvc.perform(get("/tasks").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Buy milk"))
                .andExpect(jsonPath("$[0].owner").value(username));
    }

    @Test
    void adminSeesAllTasks_usersSeeOnlyTheirOwn() throws Exception {
        // user1
        String user1 = "u1" + System.currentTimeMillis();
        register(user1, "pw");
        Cookie cookie1 = login(user1, "pw");
        mockMvc.perform(post("/tasks").with(csrf()).cookie(cookie1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Alice task\",\"done\":false}"))
                .andExpect(status().isOk());

        // user2
        String user2 = "u2" + System.currentTimeMillis();
        register(user2, "pw");
        Cookie cookie2 = login(user2, "pw");
        mockMvc.perform(post("/tasks").with(csrf()).cookie(cookie2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bob task\",\"done\":true}"))
                .andExpect(status().isOk());

        // user1 sees only own
        mockMvc.perform(get("/tasks").cookie(cookie1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].owner").value(user1));

        // user2 sees only own
        mockMvc.perform(get("/tasks").cookie(cookie2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].owner").value(user2));

        // admin sees all
        String admin = "admin" + System.currentTimeMillis();
        registerAsAdmin(admin, "pw");
        Cookie adminCookie = login(admin, "pw");
        mockMvc.perform(get("/tasks").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ------------------------------------------------------------------------
    // 🔒 TASK TESTS (SECURITY)
    // ------------------------------------------------------------------------

    @Test
    void ownerCanUpdateTask_nonOwnerCannot_adminCan() throws Exception {
        String owner = "owner" + System.currentTimeMillis();
        register(owner, "pw");
        Cookie ownerCookie = login(owner, "pw");

        // owner creates task
        MvcResult result = mockMvc.perform(post("/tasks").with(csrf()).cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Secret\",\"done\":false}"))
                .andExpect(status().isOk())
                .andReturn();
        Number taskIdNum = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        Long taskId = taskIdNum.longValue();

        // non-owner tries update -> forbidden
        String intruder = "intruder" + System.currentTimeMillis();
        register(intruder, "pw");
        Cookie intruderCookie = login(intruder, "pw");
        mockMvc.perform(put("/tasks/" + taskId).with(csrf()).cookie(intruderCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked\",\"done\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You are not allowed to update this task"));

        // admin can update
        String admin = "admin" + System.currentTimeMillis();
        registerAsAdmin(admin, "pw");
        Cookie adminCookie = login(admin, "pw");
        mockMvc.perform(put("/tasks/" + taskId).with(csrf()).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Reviewed\",\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Reviewed"));
    }

    @Test
    void adminCanDeleteTask_userCannot() throws Exception {
        String user = "deleteme" + System.currentTimeMillis();
        register(user, "pw");
        Cookie userCookie = login(user, "pw");

        // user creates task
        MvcResult result = mockMvc.perform(post("/tasks").with(csrf()).cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"To delete\",\"done\":false}"))
                .andExpect(status().isOk())
                .andReturn();
        Number taskIdNum = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        Long taskId = taskIdNum.longValue();

        // user cannot delete
        mockMvc.perform(delete("/tasks/" + taskId).with(csrf()).cookie(userCookie))
                .andExpect(status().isForbidden());

        // admin deletes it
        String admin = "admin" + System.currentTimeMillis();
        registerAsAdmin(admin, "pw");
        Cookie adminCookie = login(admin, "pw");
        mockMvc.perform(delete("/tasks/" + taskId).with(csrf()).cookie(adminCookie))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------------
    // 🛠 HELPERS
    // ------------------------------------------------------------------------

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_USER"));
    }

    private void registerAsAdmin(String username, String password) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("role", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_ADMIN"));
    }

    private Cookie login(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.user.username").value(username))
                .andReturn();

        return loginResult.getResponse().getCookie("JWT");
    }
}
