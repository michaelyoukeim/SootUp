package de.upb.soot.core;
/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1997 - 1999 Raja Vallee-Rai
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import de.upb.soot.jimple.common.type.RefType;
import de.upb.soot.jimple.common.type.Type;
import de.upb.soot.namespaces.classprovider.AbstractClassSource;
import de.upb.soot.signatures.JavaClassSignature;
import de.upb.soot.validation.ClassFlagsValidator;
import de.upb.soot.validation.ClassValidator;
import de.upb.soot.validation.MethodDeclarationValidator;
import de.upb.soot.validation.OuterClassValidator;
import de.upb.soot.validation.ValidationException;
import de.upb.soot.views.IView;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/*
 * Incomplete and inefficient implementation.
 *
 * Implementation notes:
 *
 * 1. The getFieldOf() method is slow because it traverses the list of fields, comparing the names,
 * one by one.  If you establish a Dictionary of Name->Field, you will need to add a
 * notifyOfNameChange() method, and register fields which belong to classes, because the hashtable
 * will need to be updated.  I will do this later. - kor  16-Sep-97
 *
 * 2. Note 1 is kept for historical (i.e. amusement) reasons.  In fact, there is no longer a list of fields;
 * these are kept in a Chain now.  But that's ok; there is no longer a getFieldOf() method,
 * either.  There still is no efficient way to get a field by name, although one could establish
 * a Chain of EquivalentValue-like objects and do an O(1) search on that.  - plam 2-24-00
 */

/**
 * Soot's counterpart of the source languages class concept. Soot representation of a Java class. They are usually created by
 * a Scene, but can also be constructed manually through the given constructors.
 *
 * @author Manuel Benz created on 06.06.18.
 * @author Linghui Luo
 */

public class SootClass extends AbstractClass implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -4145583783298080555L;

  private final ResolvingLevel resolvingLevel;
  private final ClassType classType;
  private final Position position;
  private final EnumSet<Modifier> modifiers;
  private final RefType refType;
  private final JavaClassSignature classSignature;
  private final Set<SootField> fields;
  private final Set<SootMethod> methods;
  private final Set<JavaClassSignature> interfaces;
  private final Optional<JavaClassSignature> superClass;
  private final Optional<JavaClassSignature> outerClass;

  public final static String INVOKEDYNAMIC_DUMMY_CLASS_NAME = "soot.dummy.InvokeDynamic";

  public SootClass(IView view, ResolvingLevel resolvingLevel, AbstractClassSource classSource, ClassType type,
      Optional<JavaClassSignature> superClass, Set<JavaClassSignature> interfaces, Optional<JavaClassSignature> outerClass,
      Position position, EnumSet<Modifier> modifiers) {
    this(view, resolvingLevel, classSource, type, superClass, interfaces, outerClass, new HashSet<>(), new HashSet<>(),
        position, modifiers);
  }

  public SootClass(IView view, ResolvingLevel resolvingLevel, AbstractClassSource classSource, ClassType type,
      Optional<JavaClassSignature> superClass, Set<JavaClassSignature> interfaces, Optional<JavaClassSignature> outerClass,
      Set<SootField> fields, Set<SootMethod> methods, Position position, EnumSet<Modifier> modifiers) {
    super(view, classSource);
    view.addClass(this);
    this.resolvingLevel = resolvingLevel;
    this.classType = type;
    this.superClass = superClass;
    this.interfaces = interfaces;
    this.classSignature = classSource.getClassSignature();
    this.refType = view.getRefType(classSignature.getFullyQualifiedName());
    refType.setSootClass(this);
    this.outerClass = outerClass;
    this.position = position;
    this.modifiers = modifiers;
    this.fields = fields;
    this.methods = methods;
    for (SootField field : fields) {
      field.setDeclaringClass(this);
    }
    for (SootMethod method : methods) {
      method.setDeclaringClass(this);
    }
  }

  public void resolve(de.upb.soot.core.ResolvingLevel resolvingLevel) {
    this.getClassSource().getContent().resolve(resolvingLevel, getView());
  }

  /**
   * Checks if the class has at lease the resolving level specified. This check does nothing is the class resolution process
   * is not completed.
   *
   * @param level
   *          the resolution level, one of DANGLING, HIERARCHY, SIGNATURES, and BODIES
   * @throws java.lang.RuntimeException
   *           if the resolution is at an insufficient level
   */
  public void checkLevel(ResolvingLevel level) {
    // Fast check: e.g. FastHierarchy.canStoreClass calls this method quite
    // often
    ResolvingLevel currentLevel = resolvingLevel();
    if (currentLevel.ordinal() >= level.ordinal()) {
      return;
    }

    if (!this.getView().doneResolving() || this.getView().getOptions().ignore_resolving_levels()) {
      return;
    }
    checkLevelIgnoreResolving(level);
  }

  /**
   * Checks if the class has at lease the resolving level specified. This check ignores the resolution completeness.
   *
   * @param level
   *          the resolution level, one of DANGLING, HIERARCHY, SIGNATURES, and BODIES
   * @throws java.lang.RuntimeException
   *           if the resolution is at an insufficient level
   */
  public void checkLevelIgnoreResolving(ResolvingLevel level) {
    ResolvingLevel currentLevel = resolvingLevel();
    if (currentLevel.ordinal() < level.ordinal()) {
      String hint = "\nIf you are extending Soot, try to add the following call before calling soot.Main.main(..):\n"
          + "Scene.getInstance().addBasicClass(" + classSignature + "," + level + ");\n"
          + "Otherwise, try whole-program mode (-w).";
      throw new RuntimeException("This operation requires resolving level " + level + " but " + classSignature.className
          + " is at resolving level " + currentLevel + hint);
    }
  }

  public ResolvingLevel resolvingLevel() {
    return resolvingLevel;
  }

  /**
   * Returns the number of fields in this class.
   */

  public int getFieldCount() {
    checkLevel(ResolvingLevel.SIGNATURES);
    return fields == null ? 0 : fields.size();
  }

  /**
   * Returns a backed Chain of fields.
   */
  public Collection<SootField> getFields() {
    checkLevel(ResolvingLevel.SIGNATURES);
    return fields == null ? new HashSet<>() : fields;
  }

  /**
   * Returns the field of this class with the given name and type. If the field cannot be found, an exception is thrown.
   */
  public SootField getField(String name, Type type) {
    SootField sf = getFieldUnsafe(name, type);
    if (sf == null) {
      throw new RuntimeException("No field " + name + " in class " + classSignature);
    }
    return sf;
  }

  /**
   * Returns the field of this class with the given name and type. If the field cannot be found, null is returned.
   */
  public SootField getFieldUnsafe(String name, Type type) {
    checkLevel(ResolvingLevel.SIGNATURES);
    if (fields == null) {
      return null;
    }
    for (SootField field : fields) {
      if (field.getSignature().equals(name) && field.getType().equals(type)) {
        return field;
      }
    }
    return null;
  }

  /**
   * Returns the field of this class with the given name. Throws a RuntimeException if there is more than one field with the
   * given name or if no such field exists at all.
   */
  public SootField getFieldByName(String name) {
    SootField foundField = getFieldByNameUnsafe(name);
    if (foundField == null) {
      throw new RuntimeException("No field " + name + " in class " + classSignature);
    }
    return foundField;
  }

  /**
   * Returns the field of this class with the given name. Throws a RuntimeException if there is more than one field with the
   * given name. Returns null if no field with the given name exists.
   */
  public SootField getFieldByNameUnsafe(String name) {
    checkLevel(ResolvingLevel.SIGNATURES);
    if (fields == null) {
      return null;
    }

    SootField foundField = null;
    for (SootField field : fields) {
      if (field.getSignature().equals(name)) {
        if (foundField == null) {
          foundField = field;
        } else {
          throw new RuntimeException("ambiguous field: " + name);
        }
      }
    }
    return foundField;
  }

  /**
   * Returns the field of this class with the given subsignature. If such a field does not exist, an exception is thrown.
   */
  public SootField getField(String subsignature) {
    SootField sf = getFieldUnsafe(subsignature);
    if (sf == null) {
      throw new RuntimeException("No field " + subsignature + " in class " + classSignature);
    }
    return sf;
  }

  /**
   * Returns the field of this class with the given subsignature. If such a field does not exist, null is returned.
   */
  public SootField getFieldUnsafe(String subsignature) {
    checkLevel(ResolvingLevel.SIGNATURES);
    if (fields == null) {
      return null;
    }
    for (SootField field : fields) {
      if (field.getSubSignature().equals(subsignature)) {
        return field;
      }
    }
    return null;
  }

  public Collection<SootMethod> getMethods() {
    checkLevel(ResolvingLevel.SIGNATURES);
    if (methods == null) {
      return Collections.emptySet();
    }
    return methods;
  }

  /**
   * Attempts to retrieve the method with the given name, parameters and return type. If no matching method can be found, an
   * exception is thrown.
   */
  public SootMethod getMethod(String name, List<Type> parameterTypes, Type returnType) {
    SootMethod sm = getMethodUnsafe(name, parameterTypes, returnType);
    if (sm != null) {
      return sm;
    }

    throw new RuntimeException(
        "Class " + classSignature + " doesn't have method " + name + "(" + parameterTypes + ")" + " : " + returnType);
  }

  /**
   * Attempts to retrieve the method with the given name, parameters and return type. If no matching method can be found,
   * null is returned.
   */
  public SootMethod getMethodUnsafe(String name, List<Type> parameterTypes, Type returnType) {
    checkLevel(ResolvingLevel.SIGNATURES);
    if (methods == null) {
      return null;
    }

    for (SootMethod method : methods) {
      if (method.getSignature().equals(name) && parameterTypes.equals(method.getParameterTypes())
          && returnType.equals(method.getReturnType())) {
        return method;
      }
    }
    return null;
  }

  /**
   * Attempts to retrieve the method with the given name and parameters. This method may throw an AmbiguousMethodException if
   * there is more than one method with the given name and parameter.
   */

  public SootMethod getMethod(String name, List<Type> parameterTypes) {
    checkLevel(ResolvingLevel.SIGNATURES);
    SootMethod foundMethod = null;

    if (methods == null) {
      return null;
    }

    for (SootMethod method : methods) {
      if (method.getSignature().equals(name) && parameterTypes.equals(method.getParameterTypes())) {
        if (foundMethod == null) {
          foundMethod = method;
        } else {
          throw new RuntimeException("ambiguous method");
        }
      }
    }

    if (foundMethod == null) {
      throw new RuntimeException("couldn't find method " + name + "(" + parameterTypes + ") in " + this);
    }
    return foundMethod;
  }

  /**
   * Attempts to retrieve the method with the given name. This method may throw an AmbiguousMethodException if there are more
   * than one method with the given name. If no method with the given is found, null is returned.
   */
  public SootMethod getMethodByNameUnsafe(String name) {
    checkLevel(ResolvingLevel.SIGNATURES);
    SootMethod foundMethod = null;

    if (methods == null) {
      return null;
    }

    for (SootMethod method : methods) {
      if (method.getSignature().equals(name)) {
        if (foundMethod == null) {
          foundMethod = method;
        } else {
          throw new RuntimeException("ambiguous method: " + name + " in class " + this);
        }
      }
    }
    return foundMethod;
  }

  /**
   * Attempts to retrieve the method with the given name. This method may throw an AmbiguousMethodException if there are more
   * than one method with the given name. If no method with the given is found, an exception is thrown as well.
   */
  public SootMethod getMethodByName(String name) {
    SootMethod foundMethod = getMethodByNameUnsafe(name);
    if (foundMethod == null) {
      throw new RuntimeException("couldn't find method " + name + "(*) in " + this);
    }
    return foundMethod;
  }

  /**
   * Returns the modifiers of this class.
   */
  public EnumSet<Modifier> getModifiers() {
    return modifiers;
  }

  /**
   * Returns the number of interfaces being directly implemented by this class. Note that direct implementation corresponds
   * to an "implements" keyword in the Java class file and that this class may still be implementing additional interfaces in
   * the usual sense by being a subclass of a class which directly implements some interfaces.
   */

  public int getInterfaceCount() {
    checkLevel(ResolvingLevel.HIERARCHY);
    return interfaces == null ? 0 : interfaces.size();
  }

  /**
   * Returns a backed Chain of the interfaces that are directly implemented by this class. (see getInterfaceCount())
   */
  public Collection<SootClass> getInterfaces() {
    checkLevel(ResolvingLevel.HIERARCHY);
    Set<SootClass> ret = new HashSet<>();
    for (JavaClassSignature i : interfaces) {
      Optional<AbstractClass> op = this.getView().getClass(i);
      if (op.isPresent()) {
        ret.add((SootClass) op.get());
      }
    }
    return ret;
  }

  /**
   * Does this class directly implement the given interface? (see getInterfaceCount())
   */

  public boolean implementsInterface(JavaClassSignature classSignature) {
    checkLevel(ResolvingLevel.HIERARCHY);
    if (interfaces == null) {
      return false;
    }

    for (JavaClassSignature sc : interfaces) {
      if (sc.equals(classSignature)) {
        return true;
      }
    }
    return false;
  }

  /**
   * WARNING: interfaces are subclasses of the java.lang.Object class! Does this class have a superclass? False implies that
   * this is the java.lang.Object class. Note that interfaces are subclasses of the java.lang.Object class.
   */

  public boolean hasSuperclass() {
    checkLevel(ResolvingLevel.HIERARCHY);
    return superClass.isPresent();
  }

  /**
   * WARNING: interfaces are subclasses of the java.lang.Object class! Returns the superclass of this class. (see
   * hasSuperclass())
   */
  public Optional<SootClass> getSuperclass() {
    checkLevel(ResolvingLevel.HIERARCHY);
    return superClass.flatMap(s -> this.getView().getClass(s).map(c -> (SootClass) c));
  }

  public boolean hasOuterClass() {
    checkLevel(ResolvingLevel.HIERARCHY);
    return outerClass.isPresent();
  }

  /**
   * This method returns the outer class.
   */
  public Optional<SootClass> getOuterClass() {
    checkLevel(ResolvingLevel.HIERARCHY);
    return outerClass.flatMap(s -> this.getView().getClass(s).map(c -> (SootClass) c));
  }

  public boolean isInnerClass() {
    return hasOuterClass();
  }

  /**
   * Returns the ClassSignature of this class.
   */
  @Override
  public JavaClassSignature getSignature() {
    return classSignature;
  }

  /** Convenience method; returns true if this class is an interface. */
  public boolean isInterface() {
    checkLevel(ResolvingLevel.HIERARCHY);
    return Modifier.isInterface(this.getModifiers());
  }

  /** Convenience method; returns true if this class is an enumeration. */
  public boolean isEnum() {
    checkLevel(ResolvingLevel.HIERARCHY);
    return Modifier.isEnum(this.getModifiers());
  }

  /** Convenience method; returns true if this class is synchronized. */
  public boolean isSynchronized() {
    checkLevel(ResolvingLevel.HIERARCHY);
    return Modifier.isSynchronized(this.getModifiers());
  }

  /** Returns true if this class is not an interface and not abstract. */
  public boolean isConcrete() {
    return !isInterface() && !isAbstract();
  }

  /** Convenience method; returns true if this class is public. */
  public boolean isPublic() {
    return Modifier.isPublic(this.getModifiers());
  }

  public boolean hasRefType() {
    return refType != null;
  }

  /** Returns the RefType corresponding to this class. */
  public RefType getType() {
    return refType;
  }

  /** Returns the name of this class. */
  @Override
  public String toString() {
    return classSignature.toString();
  }

  /**
   * Returns true if this class is an application class.
   *
   * 
   */
  public boolean isApplicationClass() {
    return classType.equals(ClassType.Application);
  }

  /**
   * Returns true if this class is a library class.
   *
   */
  public boolean isLibraryClass() {
    return classType.equals(ClassType.Library);
  }

  /**
   * Sometimes we need to know which class is a JDK class. There is no simple way to distinguish a user class and a JDK
   * class, here we use the package prefix as the heuristic.
   *
   * @author xiao
   */
  private static final Pattern libraryClassPattern
      = Pattern.compile("^(?:java\\.|sun\\.|javax\\.|com\\.sun\\.|org\\.omg\\.|org\\.xml\\.|org\\.w3c\\.dom)");

  public boolean isJavaLibraryClass() {
    return libraryClassPattern.matcher(classSignature.className).find();
  }

  /**
   * Returns true if this class is a phantom class.
   *
   * 
   */
  public boolean isPhantomClass() {
    return classType.equals(ClassType.Phantom);
  }

  /**
   * Convenience method returning true if this class is private.
   */
  public boolean isPrivate() {
    return Modifier.isPrivate(this.getModifiers());
  }

  /**
   * Convenience method returning true if this class is protected.
   */
  public boolean isProtected() {
    return Modifier.isProtected(this.getModifiers());
  }

  /**
   * Convenience method returning true if this class is abstract.
   */
  public boolean isAbstract() {
    return Modifier.isAbstract(this.getModifiers());
  }

  /**
   * Convenience method returning true if this class is final.
   */
  public boolean isFinal() {
    return Modifier.isFinal(this.getModifiers());
  }

  /**
   * Convenience method returning true if this class is static.
   */
  public boolean isStatic() {
    return Modifier.isStatic(this.getModifiers());
  }

  protected int number = 0;

  private static ClassValidator[] validators;

  /**
   * Returns an array containing some validators in order to validate the SootClass
   *
   * @return the array containing validators
   */
  private synchronized static ClassValidator[] getValidators() {
    if (validators == null) {
      validators = new ClassValidator[] { OuterClassValidator.getInstance(), MethodDeclarationValidator.getInstance(),
          ClassFlagsValidator.getInstance() };
    }
    return validators;
  };

  /**
   * Validates this SootClass for logical errors. Note that this does not validate the method bodies, only the class
   * structure.
   */
  public void validate() {
    final List<ValidationException> exceptionList = new ArrayList<ValidationException>();
    validate(exceptionList);
    if (!exceptionList.isEmpty()) {
      throw exceptionList.get(0);
    }
  }

  /**
   * Validates this SootClass for logical errors. Note that this does not validate the method bodies, only the class
   * structure. All found errors are saved into the given list.
   */
  public void validate(List<ValidationException> exceptionList) {
    final boolean runAllValidators = this.getView().getOptions().debug() || this.getView().getOptions().validate();
    for (ClassValidator validator : getValidators()) {
      if (!validator.isBasicValidator() && !runAllValidators) {
        continue;
      }
      validator.validate(this, exceptionList);
    }
  }

  public Position getPosition() {
    return this.position;
  }

  @Override
  public AbstractClassSource getClassSource() {
    return classSource;
  }

  @Override
  public String getName() {
    return this.classSignature.getFullyQualifiedName();
  }

}
