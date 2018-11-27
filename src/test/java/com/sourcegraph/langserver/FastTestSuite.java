package com.sourcegraph.langserver;

import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;

@RunWith(Categories.class)
@Categories.ExcludeCategory(SlowTestCategory.class)
public class FastTestSuite extends AllTestSuite {
}
