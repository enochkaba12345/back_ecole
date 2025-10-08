package com.sysgepecole.demo.Models;



import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "Tab_annee")
public class Annee {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idannee;

	private String annee;
	private boolean cloturee = true;
	private Long iduser;
	
    private Long debut;

    private Long fin;

    private Long ordre;
    
    private String libelle;


    @PrePersist
    @PreUpdate
    public void genererLibelle() {
        if (debut != null && fin != null) {
            this.libelle = debut + "-" + fin; // Résultat : "2024-2025"
        }
    }


	public Long getIdannee() {
		return idannee;
	}
	public void setIdannee(Long idannee) {
		this.idannee = idannee;
	}
	public String getAnnee() {
		return annee;
	}
	public void setAnnee(String annee) {
		this.annee = annee;
	}
	public long getIduser() {
		return iduser;
	}
	public void setIduser(long iduser) {
		this.iduser = iduser;
	}
	public boolean isCloturee() {
		return cloturee;
	}
	public void setCloturee(boolean cloturee) {
		this.cloturee = cloturee;
	}


	public Long getDebut() {
		return debut;
	}


	public void setDebut(Long debut) {
		this.debut = debut;
	}


	public Long getFin() {
		return fin;
	}


	public void setFin(Long fin) {
		this.fin = fin;
	}


	public Long getOrdre() {
		return ordre;
	}


	public void setOrdre(Long ordre) {
		this.ordre = ordre;
	}


	public String getLibelle() {
		return libelle;
	}


	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}


	@Override
	public String toString() {
		return "Annee [idannee=" + idannee + ", annee=" + annee + ", cloturee=" + cloturee + ", iduser=" + iduser
				+ ", debut=" + debut + ", fin=" + fin + ", ordre=" + ordre + ", libelle=" + libelle + "]";
	}

	
	
	

}
