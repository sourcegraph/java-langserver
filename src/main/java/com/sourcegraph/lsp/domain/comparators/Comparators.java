package com.sourcegraph.lsp.domain.comparators;

import com.sourcegraph.lsp.domain.structures.*;

import javax.lang.model.element.ElementKind;
import java.util.Comparator;

/**
 * LSP domain structure comparators
 */
public interface Comparators {

    Comparator<Position> POSITION =
            Comparator.nullsFirst(Comparator.
                    comparing(Position::getLine).
                    thenComparing(Position::getCharacter));

    Comparator<Range> RANGE =
            Comparator.nullsFirst(Comparator.comparing(Range::getStart, POSITION).
                    thenComparing(Range::getEnd, POSITION));

    Comparator<Location> LOCATION =
            Comparator.nullsFirst(Comparator.comparing(Location::getUri, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(Location::getRange, RANGE));

    Comparator<SymbolDescriptor> SYMBOL_DESCRIPTOR =
            Comparator.nullsFirst(Comparator.comparing(SymbolDescriptor::getQualifiedName, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(SymbolDescriptor::getElementKind, Comparator.nullsFirst(ElementKind::compareTo)));

    Comparator<ReferenceInformation> REFERENCE_INFORMATION =
            Comparator.nullsFirst(Comparator.comparing(ReferenceInformation::getSymbol, SYMBOL_DESCRIPTOR).
                    thenComparing(ReferenceInformation::getReference, LOCATION));

    Comparator<PackageDescriptor> PACKAGE_DESCRIPTOR =
            Comparator.nullsFirst(Comparator.comparing(PackageDescriptor::getRepoURL, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(PackageDescriptor::getType, Comparator.nullsFirst(PackageIdentifier.Type::compareTo)).
                    thenComparing(PackageDescriptor::getCommit, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(PackageDescriptor::getVersion, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(PackageDescriptor::getId, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(PackageDescriptor::getBaseDir, Comparator.nullsFirst(String::compareTo)));

    Comparator<PackageIdentifier> PACKAGE_IDENTIFIER =
            Comparator.nullsFirst(Comparator.comparing(PackageIdentifier::getRepoURL, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(PackageIdentifier::getType, Comparator.nullsFirst(PackageIdentifier.Type::compareTo)).
                    thenComparing(PackageIdentifier::getCommit, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(PackageIdentifier::getVersion, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(PackageIdentifier::getId, Comparator.nullsFirst(String::compareTo)));

    Comparator<PackageInformation> PACKAGE_INFORMATION =
            Comparator.nullsFirst(Comparator.comparing(PackageInformation::getPackage, PACKAGE_DESCRIPTOR));

    Comparator<SymbolInformation> SYMBOL_INFORMATION =
            Comparator.nullsFirst(Comparator.comparing(SymbolInformation::getName, Comparator.nullsFirst(String::compareTo)).
                    thenComparing(SymbolInformation::getKind, Comparator.nullsFirst(SymbolKind::compareTo)).
                    thenComparing(SymbolInformation::getLocation, LOCATION).
                    thenComparing(SymbolInformation::getContainerName, Comparator.nullsFirst(String::compareTo)));

    Comparator<SymbolLocationInformation> SYMBOL_LOCATION_INFORMATION =
            Comparator.nullsFirst(Comparator.comparing(SymbolLocationInformation::getSymbol, SYMBOL_DESCRIPTOR).
                    thenComparing(SymbolLocationInformation::getLocation, LOCATION));

    Comparator<DependencyReference> DEPENDENCY_REFERENCE =
            Comparator.nullsFirst(Comparator.comparing(DependencyReference::getAttributes, PACKAGE_IDENTIFIER));


}