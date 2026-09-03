package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.mapper.UserMapper;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User createUser(Long id, String username, String email, String password, Boolean activated) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setActivated(activated);
        return user;
    }

    @Test
    void shouldReturnAllUsers() {
        var u1 = createUser(1L, "jdoe", "jdoe@example.com", "123", true);
        var u2 = createUser(2L, "jane", "jane@example.com", "456", false);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> users = userService.findAll();

        assertThat(users).hasSize(2);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnUserById() {
        var user = createUser(1L, "jdoe", "jdoe@example.com", "123", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> found = userService.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("jdoe");
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> found = userService.findById(99L);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldSaveUser() {
        var user = createUser(null, "newuser", "new@example.com", "789", true);
        var saved = createUser(1L, "newuser", "new@example.com", "789", true);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.save(user);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("newuser");
    }

    @Test
    void shouldUpdateExistingUser() {
        var existing = createUser(1L, "old", "old@example.com", "123", true);
        var updated = createUser(1L, "new", "new@example.com", "456", false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(updated);

        User result = userService.update(1L, updated);

        assertThat(result.getUsername()).isEqualTo("new");
        verify(userRepository, times(1)).save(updated);
    }

    @Test
    void shouldThrowWhenUpdatingNonexistent() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        var user = createUser(99L, "x", "x@example.com", "789", true);

        assertThatThrownBy(() -> userService.update(99L, user))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldDeleteUserById() {
        var user = createUser(1L, "todelete", "del@example.com", "123", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteById(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonexistent() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteById(99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");

        verify(userRepository, never()).deleteById(any());
    }
}
