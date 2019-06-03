# Auto-generated Mappers Unit Tests

Since test cases for Mapstruct mappers are usually the same across different projects, we can leverage code generation to generate the test cases for us. The goal is to provide a tool which can generate the test cases but also allows custom test cases to allow developers to add their own should they need to.

## Data Generation
The tool should use a [random data generator](https://github.com/j-easy/easy-random) to generate its test data. However, since in some cases manual data modification is needed, it should allow developers to operate on generate data.

### @DataGenerator
If you need to generate objects of a specific type yourself, you can use `@DataGenerator` on a method and based on its return type it will be used to generate the data for that type.
```java
@DataGenerator
ActionFigureBO actionFigureBO() {
  return new ActionFigureBO();
}
```

### @AfterDataGeneration
This annotation lets developers add methods which take a generated object and modify it (or ignore it and return a totally custom one). Below are two examples of how it can be used.

```java
@AfterDataGeneration
AbstractDataBO modifyAbstractBO(final AbstractDataBO generated) {
  return generated.withDeleted(false);
}

@AfterDataGeneration
SpecialDataBO modifySpecialBO(final SpecialDataBO generated) {
  return generated.withDeleted(false)
          .withChannel(SomeEnum.SomeValue);
}
```

The most specific method will be applied for a certain type. All sub-classes of `AbstractDataBO` will go through `modifyAbstractBO()` but since `SpecialDataBO` has its own method it will go through `modifySpecialBO()` instead.

## Generated Test Cases
