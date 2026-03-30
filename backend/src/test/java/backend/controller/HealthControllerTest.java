package backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTest {

  private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();

  @Test
  void healthReturnsOk() throws Exception {
    mockMvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
  }

  @Test
  void pingReturnsOk() throws Exception {
    mockMvc.perform(get("/ping"))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
  }

  @Test
  void pingHeadReturnsOk() throws Exception {
    mockMvc.perform(head("/ping"))
        .andExpect(status().isOk());
  }
}
