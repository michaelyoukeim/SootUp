package de.upb.soot.example;

import de.upb.soot.Project;
import de.upb.soot.Scope;
import de.upb.soot.callgraph.CallGraph;
import de.upb.soot.inputlocation.AnalysisInputLocation;
import de.upb.soot.inputlocation.JavaClassPathAnalysisInputLocation;
import de.upb.soot.inputlocation.JavaSourcePathAnalysisInputLocation;
import de.upb.soot.typehierarchy.TypeHierarchy;
import de.upb.soot.views.View;
import java.util.Collections;

/**
 * A sample application to illustrate a potential client's path through the API
 *
 * @author Linghui Luo
 * @author Ben Hermann
 */
public class SimpleSootClient {

  public static void main(String[] args) {
    String javaClassPath = "example/classes/";
    String javaSourcePath = "example/src";

    AnalysisInputLocation cpBased = new JavaClassPathAnalysisInputLocation(javaClassPath);

    AnalysisInputLocation walaSource =
        new JavaSourcePathAnalysisInputLocation(Collections.singleton(javaSourcePath));

    Project<AnalysisInputLocation> p = new Project<>(walaSource);

    // 1. simple case
    View fullView = p.createFullView();

    CallGraph cg = fullView.createCallGraph();
    TypeHierarchy t = fullView.typeHierarchy();

    // here goes my own analysis

    // 2. advanced case
    Scope s = new Scope(cpBased);
    View limitedView = p.createView(s);

    cg = limitedView.createCallGraph();
    t = limitedView.typeHierarchy();

    // here goes my own analysis
  }
}
