package ar.edu.um.ingenieria.limitador.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.repository.UserDataRepository;

@Service
public class UserDataServiceImpl implements UserDataService {

    private final UserDataRepository userDataRepository;

    public UserDataServiceImpl(UserDataRepository userDataRepository) {
        this.userDataRepository = userDataRepository;
    }

    @Override
    public List<UserData> findAll() {
        return userDataRepository.findAll();
    }

    @Override
    public Optional<UserData> findById(Long id) {
        return userDataRepository.findById(id);
    }

    @Override
    public UserData save(UserData userData) {
        return userDataRepository.save(userData);
    }

    @Override
    public UserData update(Long id, UserData userData) {
        if (!userDataRepository.findById(id).isPresent()) {
            throw new RuntimeException("UserData not found with id: " + id);
        }
        userData.setId(id);
        return userDataRepository.save(userData);
    }

    @Override
    public void deleteById(Long id) {
        if (!userDataRepository.findById(id).isPresent()) {
            throw new RuntimeException("UserData not found with id: " + id);
        }
        userDataRepository.deleteById(id);
    }
}
