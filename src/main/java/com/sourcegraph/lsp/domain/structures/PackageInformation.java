package com.sourcegraph.lsp.domain.structures;

import java.util.Comparator;
import java.util.List;

/**
 * Created by beyang on 1/29/17.
 */
public class PackageInformation implements SourceGenerable {

    public static PackageInformation of(PackageDescriptor packageDescriptor, List<DependencyReference> dependencies) {
        PackageInformation p = new PackageInformation();
        p.setPackage(packageDescriptor);
        p.setDependencies(dependencies);
        return p;
    }

    public static Comparator<PackageInformation> comparator = (a, b) -> {
        return PackageIdentifier.comparator.compare(a.packageDescriptor.getIdentifier(), b.packageDescriptor.getIdentifier());
    };

    private PackageDescriptor packageDescriptor;
    private List<DependencyReference> dependencies;

    public PackageDescriptor getPackage() {
        return packageDescriptor;
    }

    public void setPackage(PackageDescriptor packageDescriptor) {
        this.packageDescriptor = packageDescriptor;
    }

    public List<DependencyReference> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<DependencyReference> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public String generateSource(String linePrefix) {
        return String.format("%s.of(%s, %s)", getClass().getSimpleName(),
                SourceGenerable.q(packageDescriptor, linePrefix),
                SourceGenerable.q(dependencies, linePrefix)
        );
    }
}
