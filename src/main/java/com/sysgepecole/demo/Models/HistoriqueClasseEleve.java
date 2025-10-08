package com.sysgepecole.demo.Models;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Tab_Historique_classe_eleve")
public class HistoriqueClasseEleve {
	
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ideleve;
    private Long idclasse;
    private Long idannee;

    private Date datePassation=new Date();
    private Long iduser;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public Date getDatePassation() {
		return datePassation;
	}
	public void setDatePassation(Date datePassation) {
		this.datePassation = datePassation;
	}
	public Long getIduser() {
		return iduser;
	}
	public void setIduser(Long iduser) {
		this.iduser = iduser;
	}
	public Long getIdeleve() {
		return ideleve;
	}
	public void setIdeleve(Long ideleve) {
		this.ideleve = ideleve;
	}
	public Long getIdclasse() {
		return idclasse;
	}
	public void setIdclasse(Long idclasse) {
		this.idclasse = idclasse;
	}
	public Long getIdannee() {
		return idannee;
	}
	public void setIdannee(Long idannee) {
		this.idannee = idannee;
	}

	


	
	
    
    
    

}
