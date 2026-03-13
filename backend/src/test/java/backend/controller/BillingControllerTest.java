package backend.controller;

import backend.dto.CheckoutResponse;
import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.BillingService;
import com.stripe.model.billingportal.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillingControllerTest {

  private MockMvc mvc;

  @Mock
  private BillingService billingService;

  @Mock
  private UserRepository userRepository;

  private User mockUser;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    BillingController controller = new BillingController(billingService, userRepository);
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
    mockUser = Mockito.mock(User.class);
    Mockito.when(mockUser.getId()).thenReturn(1L);
    Mockito.when(mockUser.getEmail()).thenReturn("test@example.com");
  }

  private UsernamePasswordAuthenticationToken authPrincipal() {
    return new UsernamePasswordAuthenticationToken(mockUser, null);
  }

  @Test
  void statusEndpointOk() throws Exception {
    Session portal = Mockito.mock(Session.class);
    Mockito.when(billingService.isConfigured()).thenReturn(true);
    Mockito.when(billingService.refreshStatus(Mockito.any())).thenReturn("active");
    Mockito.when(billingService.createPortal(Mockito.any())).thenReturn(portal);
    Mockito.when(portal.getUrl()).thenReturn("http://example.com");

    mvc.perform(get("/billing/status").principal(authPrincipal()))
        .andExpect(status().isOk());
  }

  @Test
  void checkoutEndpointOk() throws Exception {
    Mockito.when(billingService.isConfigured()).thenReturn(true);
    Mockito.when(billingService.createCheckout(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(com.stripe.model.checkout.Session.class));

    mvc.perform(post("/billing/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
            .principal(authPrincipal()))
        .andExpect(status().isOk());
  }
}
