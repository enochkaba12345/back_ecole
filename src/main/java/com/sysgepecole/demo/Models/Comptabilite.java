package com.sysgepecole.demo.Models;

import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tab_comptables")
public class Comptabilite {
	
	
	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    private String libelle;
	    private Double montant;
	    private Double montants;
	    private Boolean annule;
	    private String typeoperation; 
	    private String etape; 
	    @Column(nullable = true)
	    private LocalDate dateOperation ;
	    @Column(nullable = true)
	    private LocalDate dateOperationmodi;
	    private Long iduser;
	    private Long idusermodi;
	    private Long idannee;		
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public String getLibelle() {
			return libelle;
		}
		public void setLibelle(String libelle) {
			this.libelle = libelle;
		}
		public Double getMontant() {
			return montant;
		}
		public void setMontant(Double montant) {
			this.montant = montant;
		}
		public String getTypeoperation() {
			return typeoperation;
		}
		public void setTypeoperation(String typeoperation) {
			this.typeoperation = typeoperation;
		}
		public String getEtape() {
			return etape;
		}
		public void setEtape(String etape) {
			this.etape = etape;
		}
		public LocalDate getDateOperation() {
			return dateOperation;
		}
		public void setDateOperation(LocalDate now) {
			this.dateOperation = now;
		}
		public LocalDate getDateOperationmodi() {
			return dateOperationmodi;
		}
		public void setDateOperationmodi(LocalDate localDate) {
			this.dateOperationmodi = localDate;
		}
		public Long getIduser() {
			return iduser;
		}
		public void setIduser(Long iduser) {
			this.iduser = iduser;
		}
		public Long getIdusermodi() {
			return idusermodi;
		}
		public void setIdusermodi(Long idusermodi) {
			this.idusermodi = idusermodi;
		}
		public Long getIdannee() {
			return idannee;
		}
		public void setIdannee(Long idannee) {
			this.idannee = idannee;
		}
		
		
		public Double getMontants() {
			return montants;
		}
		public void setMontants(Double montants) {
			this.montants = montants;
		}
		
		
		public Boolean getAnnule() {
			return annule;
		}
		public void setAnnule(Boolean annule) {
			this.annule = annule;
		}
		@Override
		public String toString() {
			return "Comptabilite [id=" + id + ", libelle=" + libelle + ", montant=" + montant + ", montants=" + montants
					+ ", annule=" + annule + ", typeoperation=" + typeoperation + ", etape=" + etape
					+ ", dateOperation=" + dateOperation + ", dateOperationmodi=" + dateOperationmodi + ", iduser="
					+ iduser + ", idusermodi=" + idusermodi + ", idannee=" + idannee + "]";
		}
	    
	    
	    
	    



}
