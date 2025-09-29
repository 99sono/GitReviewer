## 🌳 Parsing Tree Overview

Here’s the exhaustive breakdown of the tree structure your parser should build from this class:

### 🧩 `CompilationUnitNode`

- **Start Line**: `1`
- **End Line**: `59`
- **Method**: `getJavaChunk()` → returns full class text
- **Children**:
  - `PackageDeclarationNode`
  - `ImportDeclarationNode[]`
  - `ClassDeclarationNode`

---

### 📦 `PackageDeclarationNode`

- **Start/End Line**: `3`
- **Value**: `package com.example.animals;`

---

### 📥 `ImportDeclarationNode[]`

Each import is a child of the package declaration:

| Line | Import |
|------|--------|
| 5    | `import jakarta.persistence.Entity;` |
| 6    | `import jakarta.persistence.Id;` |
| 7    | `import jakarta.persistence.Table;` |
| 8    | `import jakarta.persistence.Inheritance;` |
| 9    | `import jakarta.persistence.InheritanceType;` |
| 10   | `import jakarta.validation.constraints.AssertTrue;` |

---

### 🧱 `ClassDeclarationNode`

- **Start Line**: `16`
- **End Line**: `59`
- **Name**: `Animal`
- **Methods**:
  - `getJavaChunk()`
  - `getJavaChunkWithJavadocAndAnnotations()`
- **Linked Metadata**:
  - **JavadocNode** (Lines `12–15`)
  - **AnnotationNode[]**:
    - `@Entity` (Line `11`)
    - `@Table(name = "animals")` (Line `12`)
    - `@Inheritance(...)` (Line `13`)
- **Children**:
  - `FieldDeclarationNode` → `id`
  - `FieldDeclarationNode` → `animalTypeName`
  - `ConstructorDeclarationNode` → `Animal()`
  - `MethodDeclarationNode` → `getId()`
  - `MethodDeclarationNode` → `getAnimalTypeName()`
  - `MethodDeclarationNode` → `setAnimalTypeName(...)`
  - `MethodDeclarationNode` → `isAnimalTypeValid()` (with `@AssertTrue`)

---

## 🧬 Field Nodes

### `FieldDeclarationNode`: `private long id;`

- **Start Line**: `22`
- **End Line**: `22`
- **Linked JavadocNode**: Lines `18–20`
- **Linked AnnotationNode**: `@Id` (Line `21`)

### `FieldDeclarationNode`: `private String animalTypeName;`

- **Start Line**: `26`
- **End Line**: `26`
- **Linked JavadocNode**: Lines `23–25`
- **No annotations**

---

## 🛠️ Constructor Node

### `ConstructorDeclarationNode`: `public Animal()`

- **Start Line**: `29`
- **End Line**: `32`
- **No annotations**
- **No Javadoc**

---

## 🧪 Method Nodes

### `MethodDeclarationNode`: `getId()`

- **Start Line**: `35`
- **End Line**: `37`
- **No annotations**
- **Linked JavadocNode**: Lines `33–34`

### `MethodDeclarationNode`: `getAnimalTypeName()`

- **Start Line**: `40`
- **End Line**: `42`
- **Linked JavadocNode**: Lines `38–39`

### `MethodDeclarationNode`: `setAnimalTypeName(...)`

- **Start Line**: `45`
- **End Line**: `47`
- **Linked JavadocNode**: Lines `43–44`

### `MethodDeclarationNode`: `isAnimalTypeValid()`

- **Start Line**: `52`
- **End Line**: `54`
- **Linked JavadocNode**: Lines `48–51`
- **Linked AnnotationNode**: `@AssertTrue` (Line `51`)

---

## 🧠 AbstractNode Design

All nodes inherit from `AbstractNode`, which defines:

- `startLine: int`
- `endLine: int`
- `getJavaChunk(): String`
- `getJavaChunkWithJavadocAndAnnotations(): String` (for class, field, method nodes)

### Specialized Subclasses

- `JavaElementNode` → supports Javadoc + Annotations
  - `ClassDeclarationNode`
  - `FieldDeclarationNode`
  - `MethodDeclarationNode`
- `AnnotationNode` → metadata only
- `JavadocNode` → comment block only
