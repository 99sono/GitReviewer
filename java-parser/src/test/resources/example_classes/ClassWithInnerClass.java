/**
 * Test class demonstrating nested class structures for JavaParser validation.
 * Contains an inner class to test hierarchical AST building capabilities.
 */
public class ClassWithInnerClass {

    /** A basic field in the outer class */
    private String basicField;

    /**
     * A basic method in the outer class.
     * @return void
     */
    public void basicMethod() {}

    /**
     * Inner class demonstrating nested class structures.
     * Should be properly represented as a child of the outer class in the AST.
     */
    public class InnerClass {

        /** A basic field in the inner class */
        private String basicField2;

        /**
         * A basic method in the inner class.
         * @return void
         */
        public void basicMethod2() {}
    }
}
