package de.upb.swt.soot.java.bytecode.minimaltestsuite.java6;

import categories.Java8Test;
import de.upb.swt.soot.core.model.SootMethod;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.java.bytecode.minimaltestsuite.MinimalBytecodeTestSuiteBase;
import de.upb.swt.soot.java.core.JavaIdentifierFactory;
import de.upb.swt.soot.java.core.types.JavaClassType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** @author Kaustubh Kelkar */
@Category(Java8Test.class)
public class DeclareInnerClassTest extends MinimalBytecodeTestSuiteBase {

  final JavaClassType innerClassType =
      JavaIdentifierFactory.getInstance()
          .getClassType(getDeclaredClassSignature().getFullyQualifiedName() + "$InnerClass");

  @Override
  public MethodSignature getMethodSignature() {
    return identifierFactory.getMethodSignature(
        getDeclaredClassSignature(), "methodDisplayOuter", "void", Collections.emptyList());
  }

  public MethodSignature getInnerMethodSignature() {
    return identifierFactory.getMethodSignature(
        innerClassType, "methodDisplayInner", "void", Collections.emptyList());
  }

  @Test
  public void test() {
    SootMethod method = loadMethod(getMethodSignature());
    assertJimpleStmts(method, expectedBodyStmts());

    method = loadMethod(getInnerMethodSignature());
    assertJimpleStmts(method, expectedInnerClassBodyStmts());
  }

  /**
   *
   *
   * <pre>
   *     public void methodDisplayOuter(){
   *         System.out.println("methodDisplayOuter");
   *     }
   * </pre>
   */
  @Override
  public List<String> expectedBodyStmts() {
    return Stream.of(
            "l0 := @this: DeclareInnerClass",
            "$stack1 = <java.lang.System: java.io.PrintStream out>",
            "virtualinvoke $stack1.<java.io.PrintStream: void println(java.lang.String)>(\"methodDisplayOuter\")",
            "return")
        .collect(Collectors.toList());
  }

  /**
   *
   *
   * <pre>
   *     public void methodDisplayInner(){
   *             System.out.println("methodDisplayInner");
   *         }
   * </pre>
   */
  public List<String> expectedInnerClassBodyStmts() {
    return Stream.of(
            "l0 := @this: DeclareInnerClass$InnerClass",
            "$stack1 = <java.lang.System: java.io.PrintStream out>",
            "virtualinvoke $stack1.<java.io.PrintStream: void println(java.lang.String)>(\"methodDisplayInner\")",
            "return")
        .collect(Collectors.toList());
  }
}
