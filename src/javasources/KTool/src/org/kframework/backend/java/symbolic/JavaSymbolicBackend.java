package org.kframework.backend.java.symbolic;

import org.kframework.backend.BasicBackend;
import org.kframework.backend.java.indexing.IndexingTable;
import org.kframework.backend.java.indexing.pathIndex.PathIndex;
import org.kframework.compile.FlattenModules;
import org.kframework.compile.ResolveConfigurationAbstraction;
import org.kframework.compile.checks.CheckConfigurationCells;
import org.kframework.compile.checks.CheckRewrite;
import org.kframework.compile.checks.CheckVariables;
import org.kframework.compile.sharing.DeclareCellLabels;
import org.kframework.compile.tags.AddDefaultComputational;
import org.kframework.compile.tags.AddOptionalTags;
import org.kframework.compile.tags.AddStrictStar;
import org.kframework.compile.transformers.*;
import org.kframework.compile.utils.*;
import org.kframework.kil.ASTNode;
import org.kframework.kil.Definition;
import org.kframework.kil.loader.AddConsesVisitor;
import org.kframework.kil.loader.CollectConsesVisitor;
import org.kframework.kil.loader.CollectSubsortsVisitor;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.main.FirstStep;
import org.kframework.main.LastStep;
import org.kframework.utils.BinaryLoader;
import org.kframework.utils.Stopwatch;
import org.kframework.utils.general.GlobalSettings;

import java.io.File;
import java.io.IOException;


/**
 * Backend for the Java Rewrite Engine.
 *
 * @author AndreiS
 */
public class JavaSymbolicBackend extends BasicBackend {

    public static final String DEFINITION_FILENAME = "java_symbolic_definition.bin";
    public static final String RULE_TABLE_INDEX = "table";
    public static final String PATH_INDEX = "path";

    private class DefinitionSerializer extends CopyOnWriteTransformer {

        public DefinitionSerializer(Context context) {
            super("Serialize Compiled Definition to XML", context);
        }
        @Override
        public ASTNode transform(Definition node) throws TransformerException {
            BinaryLoader.save(new File(context.dotk, "defx-java.bin").toString(), node);

            return node;
        }

    }

    public JavaSymbolicBackend(Stopwatch sw, Context context) {
        super(sw, context);
    }

    @Override
    public Definition lastStep(Definition javaDef) {
        org.kframework.backend.java.kil.Definition definition =
                new KILtoBackendJavaKILTransformer(context, true).transformDefinition(javaDef);

        //TODO(OwolabiLeg): Add a way to measure how long it takes to build an index
        if (GlobalSettings.ruleIndex.equals(RULE_TABLE_INDEX)){
            definition.setIndex(new IndexingTable(definition));
        } else if (GlobalSettings.ruleIndex.equals(PATH_INDEX)){
            definition.setIndex(new PathIndex(definition));
        }

        assert definition.getIndex() != null;

        BinaryLoader.save(new File(context.dotk,
                JavaSymbolicBackend.DEFINITION_FILENAME).toString(),
                definition);

        return javaDef;
    }

    @Override
    public void run(Definition def) throws IOException { }

    @Override
    public String getDefaultStep() {
        return "LastStep";
    }

    @Override
    public CompilerSteps<Definition> getCompilationSteps() {
        CompilerSteps<Definition> steps = new CompilerSteps<Definition>(context);
        steps.add(new FirstStep(this, context));

        steps.add(new CheckVisitorStep<Definition>(new CheckConfigurationCells(context), context));
        steps.add(new RemoveBrackets(context));
        // SetVariablesInferredSort must be performed before AddEmptyLists
        steps.add(new SetVariablesInferredSort(context));
        steps.add(new AddEmptyLists(context));
        steps.add(new RemoveSyntacticCasts(context));
        steps.add(new CheckVisitorStep<Definition>(new CheckVariables(context), context));
        steps.add(new CheckVisitorStep<Definition>(new CheckRewrite(context), context));
        steps.add(new FlattenModules(context));

        steps.add(new CompleteSortLatice(context));
        steps.add(new CheckVisitorStep<Definition>(new AddConsesVisitor(context), context));
        steps.add(new CheckVisitorStep<Definition>(new CollectConsesVisitor(context), context));
        steps.add(new CheckVisitorStep<Definition>(new CollectSubsortsVisitor(context), context));
        steps.add(new DefinitionSerializer(context));

        steps.add(new StrictnessToContexts(context));
        steps.add(new FreezeUserFreezers(context));
        steps.add(new ContextsToHeating(context));
        //steps.add(new AddSupercoolDefinition(context));
        steps.add(new AddHeatingConditions(context));
        //steps.add(new AddSuperheatRules(context));
        steps.add(new DesugarStreams(context, true));
        steps.add(new ResolveFunctions(context));
        steps.add(new AddKCell(context));
        steps.add(new AddStreamCells(context));
        //steps.add(new AddSymbolicK(context));
        //steps.add(new AddSemanticEquality(context));
        // steps.add(new ResolveFresh());
        //steps.add(new FreshCondToFreshVar(context));
        //steps.add(new ResolveFreshVarMOS(context));

        steps.add(new AddTopCellConfig(context));
        steps.add(new AddTopCellRules(context));

        steps.add(new ResolveBinder(context));
        steps.add(new ResolveAnonymousVariables(context));
        //steps.add(new AddK2SMTLib(context));
        steps.add(new AddPredicates(context));
        //steps.add(new ResolveSyntaxPredicates(context));
        steps.add(new ResolveBuiltins(context));
        steps.add(new ResolveListOfK(context));
        steps.add(new AddInjections(context));

        steps.add(new FlattenSyntax(context));
        steps.add(new ResolveBlockingInput(context, false));
        steps.add(new InitializeConfigurationStructure(context));
        //steps.add(new AddKStringConversion(context));
        //steps.add(new AddKLabelConstant(context));
        steps.add(new ResolveHybrid(context));
        steps.add(new ResolveConfigurationAbstraction(context));
        steps.add(new ResolveOpenCells(context));
        steps.add(new ResolveRewrite(context));

        /* data structure related stuff */
        steps.add(new CompileToBuiltins(context));
        steps.add(new CompileDataStructures(context));
        steps.add(new Cell2DataStructure(context));
        steps.add(new DataStructureToLookupUpdate(context));

        //steps.add(new ResolveSupercool(context));
        steps.add(new AddStrictStar(context));
        steps.add(new AddDefaultComputational(context));
        steps.add(new AddOptionalTags(context));
        steps.add(new DeclareCellLabels(context));

        /* remove rules that are from k dist */
        steps.add(new RemovePreincludedRules(context));

        steps.add(new LastStep(this, context));

        return steps;
    }
}
