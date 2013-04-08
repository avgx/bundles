# Readme

This is an annotation processor for making Fragment construction type safe, and making saving state easier.

There are two annotations provided, `@Argument` for fragment arguments and `@Frozen` for saving and restoring state in `onSaveInstanceState`.

# @Argument
Construction of Fragments can be painful if you need to pass arguments to construct them. The problem is that you need to construct a `Bundle`,
put values in it by key and set it on the `Fragment` instance. This is not only cumbersome to do, but also not very type safe.

The `@Argument` annotation helps by generating a builder class for your `Fragment`. For example:

    :::java
    public class TestFragment extends Fragment {
        @Argument
        boolean mCheese;
        @Argument(required=false)
        int mTotal;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);        
            // Use the generated builder class to "inject" the arguments on creation
            // you can use a static import to shorten this line to injectArguments(this)
            TestFragmentBuilder.injectArguments(this);
        }
    }
    
To construct this fragment you'd do something like this:

    :::java
    TestFragment fragment = new Test2FragmentBuilder(false).build();
    TestFragment fragment2 = new Test2FragmentBuilder(false).total(10).build();

Required arguments must be passed into the builder constructor, while non-required arguments have a builder method.

# @Frozen
The `@Frozen` annotation aids with saving `Activity` or `Fragment` state in `onSaveInstanceState` and restoring it in `onCreate` or other places.
A class <YourActivityName>State is generated for this purpose. Here's an example:

    :::java
    package test;

    import static test.MyActivityState.restoreInstanceState;
    import static test.MyActivityState.saveInstanceState;
    import android.app.Activity;
    import android.os.Bundle;

    import com.neenbedankt.bundles.annotation.Frozen;

    public class MyActivity extends Activity {
        @Frozen
        String mName;
        @Frozen
        boolean mCheeseActive;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            restoreInstanceState(this, savedInstanceState);
        }

        @Override
        protected void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            saveInstanceState(this, outState);
        }
    }
    
# Using
Either build from source using Gradle, or [download the jar][1]

[1]: https://bitbucket.org/hvisser/bundles/downloads