package org.example.cloudstorage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private long id;
    @Column(name = "username", unique = true, nullable = false)
    private String username;
    @Column(name = "role", nullable = false)
    private String role;
    @Column(name = "hashed_password", nullable = false)
    private String password;

    public User(String username, String role, String password) {
        this.username = username;
        this.role = role;
        this.password = password;
    }
}
