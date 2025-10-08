package com.sysgepecole.demo.Models;

import java.time.LocalDateTime;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="tab_paiement")
public class Paiement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long idpaiement;
	
	private String frais;
	private String categorie;
	private String tranche;
	private String Statut;
	public String getFrais() {
		return frais;
	}
	public void setFrais(String frais) {
		this.frais = frais;
	}
	private Long idintermedaireclasse;
	private Long idintermedaireannee;
	private Double montants;
	private Long ideleve;
	private Date datepaie=new Date();
	private Long iduser;
	private Long idusermodi;
	
	private Boolean annule = false;

    private String motifAnnulation;
    
    private LocalDateTime datepaieannuler;
    
	public long getIdpaiement() {
		return idpaiement;
	}
	public void setIdpaiement(long idpaiement) {
		this.idpaiement = idpaiement;
	}
	

	public String getTranche() {
		return tranche;
	}
	public void setTranche(String tranche) {
		this.tranche = tranche;
	}
	public Long getIdintermedaireclasse() {
		return idintermedaireclasse;
	}
	public void setIdintermedaireclasse(Long idintermedaireclasse) {
		this.idintermedaireclasse = idintermedaireclasse;
	}
	public Long getIdintermedaireannee() {
		return idintermedaireannee;
	}
	public void setIdintermedaireannee(Long idintermedaireannee) {
		this.idintermedaireannee = idintermedaireannee;
	}
	
	public Long getIdeleve() {
		return ideleve;
	}
	public void setIdeleve(Long ideleve) {
		this.ideleve = ideleve;
	}
	public Date getDatepaie() {
		return datepaie;
	}
	public void setDatepaie(Date datepaie) {
		this.datepaie = datepaie;
	}
	
	public Long getIduser() {
		return iduser;
	}
	public void setIduser(Long iduser) {
		this.iduser = iduser;
	}
	public Double getMontants() {
		return montants;
	}
	public void setMontants(Double montants) {
		this.montants = montants;
	}
	public String getStatut() {
		return Statut;
	}
	public void setStatut(String statut) {
		Statut = statut;
	}
	public String getCategorie() {
		return categorie;
	}
	public void setCategorie(String categorie) {
		this.categorie = categorie;
	}
	@Override
	public String toString() {
		return "Paiement [idpaiement=" + idpaiement + ", frais=" + frais + ", categorie=" + categorie + ", tranche="
				+ tranche + ", Statut=" + Statut + ", idintermedaireclasse=" + idintermedaireclasse
				+ ", idintermedaireannee=" + idintermedaireannee + ", montants=" + montants + ", ideleve=" + ideleve
				+ ", datepaie=" + datepaie + ", iduser=" + iduser + ", idusermodi=" + idusermodi + ", annule=" + annule
				+ ", motifAnnulation=" + motifAnnulation + ", datepaieannuler=" + datepaieannuler + "]";
	}
	public Boolean getAnnule() {
		return annule;
	}
	public void setAnnule(Boolean annule) {
		this.annule = annule;
	}
	public String getMotifAnnulation() {
		return motifAnnulation;
	}
	public void setMotifAnnulation(String motifAnnulation) {
		this.motifAnnulation = motifAnnulation;
	}
	public LocalDateTime getDatepaieannuler() {
		return datepaieannuler;
	}
	public void setDatepaieannuler(LocalDateTime localDateTime) {
		this.datepaieannuler = localDateTime;
	}
	public Long getIdusermodi() {
		return idusermodi;
	}
	public void setIdusermodi(Long idusermodi) {
		this.idusermodi = idusermodi;
	}
	
	
	
	
	
}
