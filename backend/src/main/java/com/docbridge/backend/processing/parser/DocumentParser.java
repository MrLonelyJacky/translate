package com.docbridge.backend.processing.parser;

import com.docbridge.backend.processing.model.ParsedDocument;

public interface DocumentParser {

    ParsedDocument parse(String sourceType, String filename, byte[] content);
}

