package de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.java6;

import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.MinimalTestSuiteBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** @author Kaustubh Kelkar */
public class TernaryOperatorTest extends MinimalTestSuiteBase {
  public MethodSignature getMethodSignature() {
    return identifierFactory.getMethodSignature(
        "ternaryOperatorMethod", getDeclaredClassSignature(), "boolean", Collections.emptyList());
  }

  @Override
  public List<String> getJimpleLines() {
    return Stream.of(
            "r0 := @this: TernaryOperator",
            "$i0 = r0.<TernaryOperator: int num>",
            "$z0 = $i0 < 0",
            "if $z0 == 0 goto $z1 = 1",
            "$z1 = 0",
            "goto [?= return $z1]",
            "$z1 = 1",
            "return $z1")
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
