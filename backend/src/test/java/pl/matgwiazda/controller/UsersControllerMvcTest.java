package pl.matgwiazda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.domain.enums.UserRole;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.dto.UserUpdateCommand;
import pl.matgwiazda.service.UserService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
// Use a standalone MockMvc so we can register the AuthenticationPrincipalArgumentResolver
class UsersControllerMvcTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private AutoCloseable mocks;

    @Mock
    private UserService userService;

    private User basicUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail("u@example.com");
        user.setUserName("u");
        user.setRole(UserRole.STUDENT);
        return user;
    }

    private UsernamePasswordAuthenticationToken authFor(User user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return new UsernamePasswordAuthenticationToken(user, null, authorities);
    }

    @BeforeEach
    void setUp() {
        this.mocks = MockitoAnnotations.openMocks(this);
        this.objectMapper = new ObjectMapper();
        UsersController controller = new UsersController(userService);
        // register a test-only resolver that reads Authentication.getPrincipal() from SecurityContextHolder
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(org.springframework.security.core.annotation.AuthenticationPrincipal.class)
                                && User.class.equals(parameter.getParameterType());
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
                        var auth = SecurityContextHolder.getContext().getAuthentication();
                        return auth != null ? auth.getPrincipal() : null;
                    }
                }, new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (this.mocks != null) {
            this.mocks.close();
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMeShouldReturnUserDto() throws Exception {
        UUID id = UUID.randomUUID();
        User user = basicUser(id);

        UserDto dto = new UserDto(id, "u@example.com", "u", "STUDENT", true);
        when(userService.getUserDtoFromEntity(any(User.class))).thenReturn(dto);

        // set Authentication in SecurityContext so our resolver can return the principal to @AuthenticationPrincipal
        SecurityContextHolder.getContext().setAuthentication(authFor(user));
        mockMvc.perform(get("/api/v1/users/me").requestAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext()))
                .andDo(result -> System.out.println("AUTH (GET): " + org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("u@example.com"));

        verify(userService).getUserDtoFromEntity(any(User.class));
    }

    @Test
    void updateMeShouldReturnDto() throws Exception {
        UUID id = UUID.randomUUID();
        User user = basicUser(id);

        UserUpdateCommand cmd = new UserUpdateCommand();
        cmd.setUserName("newname");

        UserDto dto = new UserDto(id, "u@example.com", "newname", "STUDENT", true);
        when(userService.updateUser(eq(id), any(UserUpdateCommand.class))).thenReturn(dto);

        SecurityContextHolder.getContext().setAuthentication(authFor(user));
        var mvcResult = mockMvc.perform(patch("/api/v1/users/me").requestAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andDo(print())
                .andDo(result -> System.out.println("AUTH (PATCH): " + org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()))
                .andExpect(status().isOk())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        System.out.println("RESPONSE BODY (PATCH): " + body);
        // fallback assertion: ensure returned JSON contains the updated userName somewhere
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("newname"), "Response body should contain updated userName");

        // keep verifying service invocation
        verify(userService).updateUser(eq(id), any(UserUpdateCommand.class));
    }

    @Test
    void deleteMeShouldReturnNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        User user = basicUser(id);

        // ensure mock service does not throw
        org.mockito.Mockito.doNothing().when(userService).deactivateUser(id);

        SecurityContextHolder.getContext().setAuthentication(authFor(user));
        mockMvc.perform(delete("/api/v1/users/me").requestAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext()))
                .andDo(result -> System.out.println("AUTH (DELETE): " + org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userService).deactivateUser(id);
    }
}
