package com.restoria.analise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemEstoqueRepository extends JpaRepository<ItemEstoque, Long> {

    List<ItemEstoque> findByUpload(UploadPlanilha upload);
}
