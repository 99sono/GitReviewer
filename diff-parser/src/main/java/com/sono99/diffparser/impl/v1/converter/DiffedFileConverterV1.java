package com.sono99.diffparser.impl.v1.converter;

import com.sono99.diffparser.api.v1.model.DiffChunkV1;
import com.sono99.diffparser.api.v1.model.DiffedFileV1;
import com.sono99.diffparser.impl.generic.model.DiffedFile;
import com.sono99.diffparser.impl.v1.service.ConversionServiceV1;
import com.sono99.diffparser.impl.v1.service.NodeConverterV1;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Converter to transform a generic {@link DiffedFile} to a {@link DiffedFileV1}. */
@Component
public class DiffedFileConverterV1 implements NodeConverterV1<DiffedFile, DiffedFileV1> {

  @Override
  public DiffedFileV1 convert(DiffedFile source, ConversionServiceV1 conversionService) {
    List<DiffChunkV1> diffChunks =
        source.diffChunks().stream()
            .map(chunk -> conversionService.convert(chunk, DiffChunkV1.class))
            .collect(Collectors.toList());
    return new DiffedFileV1(
        source.repositoryPath(),
        source.filePath(),
        source.isCreatedNewInMergeRequest(),
        source.isDeleted(),
        source.isRenamed(),
        source.oldFilePath(),
        0, // diffStartLineForFile - not available in generic model
        diffChunks,
        ""); // diffText - not available in generic model
  }

  @Override
  public Class<DiffedFile> getSourceType() {
    return DiffedFile.class;
  }

  @Override
  public Class<DiffedFileV1> getTargetType() {
    return DiffedFileV1.class;
  }
}
