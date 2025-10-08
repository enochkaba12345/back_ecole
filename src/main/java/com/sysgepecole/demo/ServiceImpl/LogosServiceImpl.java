package com.sysgepecole.demo.ServiceImpl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sysgepecole.demo.Dto.EleveModelDto;
import com.sysgepecole.demo.Dto.LogosModelDto;
import com.sysgepecole.demo.Models.Ecole;
import com.sysgepecole.demo.Models.Logos;
import com.sysgepecole.demo.Repository.EcoleRepository;
import com.sysgepecole.demo.Repository.LogosRepository;
import com.sysgepecole.demo.Service.LogosService;



@Service
public class LogosServiceImpl implements LogosService{
	
	
	@Autowired
	private EcoleRepository ecolerepository;
	
	@Autowired
	private LogosRepository logosrepository;
	
	@Autowired 
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final String UPLOAD_DIR = "C:/logos/";

	
	@Override
	public Boolean createLogos(Logos logos) {
	    if (logos == null || logos.getIdecole() == null) {
	        throw new IllegalArgumentException("logos ou ID ecole ne peut pas être null.");
	    }

	    
	    Optional<Ecole> ecoleData = ecolerepository.findById(logos.getIdecole());
	    if (ecoleData.isEmpty()) {
	        System.err.println("Élève avec ID " + logos.getIdecole() + " introuvable.");
	        return false;
	    }

	    
	    Optional<Logos> existingLogos = logosrepository.findByIdecole(logos.getIdecole());
	    if (existingLogos.isPresent()) {
	        Logos logosToUpdate = existingLogos.get();
	        logosToUpdate.setLogos(logos.getLogos());
	        logosToUpdate.setIduser(logos.getIduser());
	        logosToUpdate.setIdecole(logos.getIdecole());
	        logosrepository.save(logosToUpdate);
	    } else {
	    	logosrepository.save(logos);
	    }

	    return true;
	}


	
	@Override
    public String uploadLogos(MultipartFile logos) throws IOException {
       
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }

        String filename = System.currentTimeMillis() + "-" + logos.getOriginalFilename();
        File file = new File(UPLOAD_DIR + filename);

        logos.transferTo(file);

        return filename;
    }
	

	@Override
	public List<LogosModelDto> collecteLogos(Long idecole) {
	    String sql = """
	        SELECT 
	            a.idecole,
	            UPPER(a.ecole) AS ecole,
	            y.id,
	            h.idcommune,
	            UPPER(h.commune) AS commune,
	            UPPER(a.avenue) AS avenue,
	            UPPER(p.province) AS province,
	            CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(y.logos, ''), 'logos.png')) AS logos
	        FROM tab_Ecole a
	        JOIN tab_Commune h ON h.idcommune = a.idcommune
	        JOIN tab_Province p ON p.idprovince = a.idprovince
	        LEFT JOIN tab_Logos y ON y.idecole = a.idecole
	        %s
	    """;

	    MapSqlParameterSource parameters = new MapSqlParameterSource();

	    String whereClause = "";
	    if (idecole != null && idecole > 0) {
	        whereClause = "WHERE a.idecole = :idecole";
	        parameters.addValue("idecole", idecole);
	    }

	    String finalQuery = String.format(sql, whereClause);

	    try {
	        return namedParameterJdbcTemplate.query(
	            finalQuery,
	            parameters,
	            new BeanPropertyRowMapper<>(LogosModelDto.class)
	        );
	    } catch (Exception e) {
	        System.err.println("Erreur lors de la récupération des logos : " + e.getMessage());
	        return Collections.emptyList();
	    }
	}

	
	



	 
	 

}
