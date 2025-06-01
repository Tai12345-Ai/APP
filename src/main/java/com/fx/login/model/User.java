package com.fx.login.model;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Data
public class User {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "id", updatable = false, nullable = false)
	private long id;

	private String fullname;
	private LocalDateTime datecreated;
	private String sex;
	private String country;
	private String email;
	private String password;

	@Enumerated(EnumType.STRING)
	private Role role;

	// ✅ THÊM: Liên kết với Resident (nếu là resident)
	@OneToOne
	@JoinColumn(name = "resident_id")
	private Resident resident;

	public enum Role {
		Admin, Resident
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", Fullname=" + fullname + ", Date created=" + datecreated + ", email=" + email + "]";
	}

	// ✅ THÊM: Helper methods để lấy thông tin từ resident
	public String getApartmentNumber() {
		return resident != null ? resident.getApartmentNumber() : null;
	}

	public String getResidentFullName() {
		return resident != null ? resident.getFullName() : fullname;
	}

	// Getters and setters (giữ nguyên code cũ)
	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public LocalDateTime getDatecreated() {
		return datecreated;
	}

	public void setDatecreated(LocalDateTime datecreated) {
		this.datecreated = datecreated;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Resident getResident() {
		return resident;
	}

	public void setResident(Resident resident) {
		this.resident = resident;
	}
}