package com.sysgepecole.demo.Models;



import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tab_classe")
public class Classe {
	

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idclasse;

	private String classe;
	private Long iduser;
	private long idniveau;
	private long ordre; // ex: 1 pour "1 ERE MATERNELLE A", 2 pour "2 EME MATERNELLE A", etc.
	private String cycle;  // ex: "MATERNELLE", "PRIMAIRE"


	public long getIdniveau() {
		return idniveau;
	}

	public void setIdniveau(long idniveau) {
		this.idniveau = idniveau;
	}

	public Long getIdclasse() {
		return idclasse;
	}

	public void setIdclasse(Long idclasse) {
		this.idclasse = idclasse;
	}

	public String getClasse() {
		return classe;
	}

	public void setClasse(String classe) {
		this.classe = classe;
	}

	public Long getIduser() {
		return iduser;
	}

	public void setIduser(Long iduser) {
		this.iduser = iduser;
	}

	public Long getOrdre() {
		return ordre;
	}

	public void setOrdre(Long ordre) {
		this.ordre = ordre;
	}

	public String getCycle() {
		return cycle;
	}

	public void setCycle(String cycle) {
		this.cycle = cycle;
	}

	@Override
	public String toString() {
		return "Classe [idclasse=" + idclasse + ", classe=" + classe + ", iduser=" + iduser + ", idniveau=" + idniveau
				+ ", ordre=" + ordre + ", cycle=" + cycle + "]";
	}

	



	
	

}
