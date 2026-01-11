package com.mockavior.contract.model;

import com.mockavior.behavior.Behavior;
import com.mockavior.core.snapshot.ContractSnapshot;

public record CompiledContract(
        ContractSnapshot snapshot,
        Behavior fallbackBehavior
) {
}