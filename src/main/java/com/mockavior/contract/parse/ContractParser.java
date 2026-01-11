package com.mockavior.contract.parse;

import com.mockavior.contract.model.RawContract;

public interface ContractParser {
    RawContract parse(String rawContent);
}
