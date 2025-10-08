package com.sysgepecole.demo.Dto;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

public class JwtResponse {

	private String token;
    private Long iduser;
    private String username;
    private String nom;
    private String postnom;
    private String prenom;
    private List<String> profil;
    
    
	public JwtResponse(String token, Long iduser, String username,String nom,String postnom,String prenom, List<String> profil, Collection<? extends GrantedAuthority> collection) {
		super();
		this.token = token;
		this.iduser = iduser;
		this.username = username;
		this.nom = nom;
		this.postnom = postnom;
		this.prenom = prenom;
		this.profil = profil;
	}


	public String getToken() {
		return token;
	}


	public void setToken(String token) {
		this.token = token;
	}


	public Long getIduser() {
		return iduser;
	}


	public void setIduser(Long iduser) {
		this.iduser = iduser;
	}


	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public List<String> getProfil() {
		return profil;
	}


	public void setProfil(List<String> profil) {
		this.profil = profil;
	}


	public String getNom() {
		return nom;
	}


	public void setNom(String nom) {
		this.nom = nom;
	}


	public String getPostnom() {
		return postnom;
	}


	public void setPostnom(String postnom) {
		this.postnom = postnom;
	}


	public String getPrenom() {
		return prenom;
	}


	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}
    
    
    
}
