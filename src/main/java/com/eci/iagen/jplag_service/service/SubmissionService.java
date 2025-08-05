package com.eci.iagen.jplag_service.service;

import org.springframework.stereotype.Service;
import com.eci.iagen.jplag_service.dto.SubmissionDto;

import java.util.List;

@Service
public class SubmissionService {
    public List<SubmissionDto> processSubmissions(List<SubmissionDto> submissions) {
        // Aquí podrías añadir lógica adicional para procesar las submissions
        return submissions;
    }
}
