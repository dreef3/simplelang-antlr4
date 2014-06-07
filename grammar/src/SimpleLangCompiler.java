import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import com.meteleva.StackMachine;
import com.meteleva.StackMachine.Command;

public class SimpleLangCompiler extends SimpleLangBaseListener {

	public static final int DATA_OFFSET = 1;
	public static final int CMDS_OFFSET = 2;
	private static final int UNKNOWN_OFFSET = -20000;
	private static final int RETURN_GOTO_OFFSET = -30000;
	private static final String DEFAULT_CONTEXT_NAME = "DEFAULT";
	private static final String ANONYMOUS_CONTEXT_NAME = "ANONYMOUS";

	private SimpleLangParser parser;
	private Map<String, Integer> parameters = new HashMap<>();
	private Map<String, FuncContext> functions = new HashMap<>();
	private List<Integer> outputCmds = new ArrayList<>();
	private List<Integer> funcCmds = new ArrayList<>();
	private ExpressionListener outputListener = new ExpressionListener(
			new FuncContext(DEFAULT_CONTEXT_NAME, parameters, outputCmds));

	private List<Integer> outputData = new ArrayList<>();

	private Map<Integer, Integer> ops = new HashMap<Integer, Integer>() {
		{
			put(SimpleLangParser.PLUS, Command.ADD.code);
			put(SimpleLangParser.MINUS, Command.MINUS.code);
			put(SimpleLangParser.MULT, Command.MULT.code);
			put(SimpleLangParser.DIV, Command.DIV.code);
		}
	};

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		new SimpleLangCompiler().run();
	}

	public void run() throws FileNotFoundException, IOException {
		initData();

		ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(
				"input.txt"));
		SimpleLangLexer lexer = new SimpleLangLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		parser = new SimpleLangParser(tokens);
		ParseTree tree = parser.program();

		ParseTreeWalker walker = new ParseTreeWalker();

		walker.walk(this, tree);

		printCommands("OUTPUT", outputCmds);

		System.out.println("DATA: ----------------------");
		for (Integer j : outputData) {
			System.out.println(j);
		}

		exec();
	}
	
	private void printCommands(String label, List<Integer> commands) {
		System.out.printf("%s COMMANDS: -----------\n", label);
		
		int i = 0;
		for (Integer cmd : commands) {
			Command c = Command.valueOf(cmd);
			if (c != null) {
				System.out.printf("%d: %d (%s)\n", i, cmd, c.name());
			} else {
				System.out.printf("%d: %d\n", i, cmd);
			}
			i++;
		}
	}

	private void initData() {
		// Functions supposed to use this as address of their returning goto
		// address
		outputData.add(0);
	}
	
	private void exec() {
		int[] data = new int[outputData.size()];
		for (int i = 0; i < outputData.size(); i++) {
			data[i] = outputData.get(i);
		}
		int[] cmds = new int[outputCmds.size()];
		for (int j = 0; j < cmds.length; j++) {
			cmds[j] = outputCmds.get(j);
		}

		new StackMachine(data, cmds).execute();
	}

	@Override
	public void enterProgram(SimpleLangParser.ProgramContext ctx) {
		//ctx.inspect(parser);
	}

	@Override
	public void enterInput(SimpleLangParser.InputContext ctx) {
		for (SimpleLangParser.ParameterContext param : ctx.parameters()
				.parameter()) {
			parameters.put(param.getText(), DATA_OFFSET + parameters.size());
			outputCmds.add(Command.READ.code);
		}
	}

	private class ExpressionListener extends SimpleLangBaseListener {
		private static final int COND_EXPR_COUNT = 4;
		private static final int GOTO_FI_SIZE = 2;
		private List<Integer> cmds;
		private Map<String, Integer> params;
		private String name;

		public ExpressionListener(FuncContext context) {
			name = context.name;
			cmds = context.cmds;
			params = context.params;
		}
		
		@Override
		public void enterExpression(SimpleLangParser.ExpressionContext ctx) {
			if (ctx.call() != null) {
				parseCall(ctx.call());
			} else if (ctx.conditional() != null) {
				parseConditional(ctx.conditional());
			} else {
				for (SimpleLangParser.ExpressionContext expr : ctx.expression()) {
					enterExpression(expr);
				}
				if (ctx.ID() != null) {
					parseVar(ctx.ID());
				} else if (ctx.NUMBER() != null) {
					cmds.add(Integer.parseInt(ctx.NUMBER().getText()));
				} else {
					for (Entry<Integer, Integer> op : ops.entrySet()) {
						if (ctx.getToken(op.getKey(), 0) != null) {
							cmds.add(op.getValue());
						}
					}
				}
			}

		}

		private void parseConditional(SimpleLangParser.ConditionalContext cond) {
			List<Integer> condCmds = new ArrayList<>();
			
			List<SimpleLangParser.ExpressionContext> expressions = cond.expression();
			if (expressions.size() != COND_EXPR_COUNT) {
				throw new IllegalArgumentException();
			}
			
			List<Integer> left = new ArrayList<>();
			new ExpressionListener(new FuncContext(name, params, left)).enterExpression(expressions.get(0));
			List<Integer> right = new ArrayList<>();
			new ExpressionListener(new FuncContext(name, params, right)).enterExpression(expressions.get(1));
			List<Integer> trueBranch = new ArrayList<>();
			new ExpressionListener(new FuncContext(name, params, trueBranch)).enterExpression(expressions.get(2));
			List<Integer> falseBranch = new ArrayList<>();
			new ExpressionListener(new FuncContext(name, params, falseBranch)).enterExpression(expressions.get(3));
			
			// IF (left) > (right) THEN (trueBranch) ELSE (falseBranch)
			
			int trueBranchOffset = cmds.size() + left.size() + right.size() + falseBranch.size() + GOTO_FI_SIZE;
						
			condCmds.add(trueBranchOffset);
			condCmds.addAll(right);
			condCmds.addAll(left);
			
			// IF< (IF=): (trueBranchOffset) (right) (left) on data stack 
			
			if (cond.EQ() != null) {
				condCmds.add(Command.IFEQ.code);
			} else {
				condCmds.add(Command.IFLT.code);
			}
			
			condCmds.addAll(falseBranch);
			
			// If true, go to true branch. Else continue on false branch, then skip true branch
			
			condCmds.add(trueBranchOffset + trueBranch.size() + 2);
			condCmds.add(Command.GOTO.code);
			
			// True branch is in the end: (right) (left) (false) (goto_fi) (true) (fi)
			
			condCmds.addAll(trueBranch);
			
			cmds.addAll(condCmds);
		}

		private void parseCall(SimpleLangParser.CallContext ctx) {
			if (true) {
				return;
			}
			
			List<List<Integer>> callCmds = new ArrayList<>();
			
			String name = ctx.ID().getText();
			if (!this.name.equals(name) && !functions.containsKey(name)) {
				throw new IllegalArgumentException();
			}
			
			FuncContext func;
			if (this.name.equals(name)) {
				//func = new FuncContext(name, params, cmds);
				return;
			} else if (functions.containsKey(name)) {
				func = functions.get(name); 
			} else {
				throw new IllegalArgumentException();
			}
			
			func = new FuncContext(func);
			
			List<SimpleLangParser.ExpressionContext> expressions = ctx.expressions().expression();
			if (func.params.size() != expressions.size()) {
				throw new IllegalArgumentException();
			}
			
			for (SimpleLangParser.ExpressionContext expr : expressions) {
				List<Integer> call = new ArrayList<>();
				new ExpressionListener(anonymous(call, params)).enterExpression(expr);
				callCmds.add(call);
				cmds.addAll(call);
			}
		}

		private void parseVar(TerminalNode id) {
			String name = id.getText();
			if (!params.containsKey(name)) {
				throw new IllegalArgumentException();
			}

			cmds.add(params.get(name));
			cmds.add(Command.LOAD.code);

		}
	}

	@Override
	public void enterOutput(SimpleLangParser.OutputContext ctx) {
		for (SimpleLangParser.ExpressionContext expr : ctx.expressions()
				.expression()) {

			outputListener.enterExpression(expr);
			outputCmds.add(Command.PRINT.code);
			outputCmds.add(Command.FREE.code);
		}

		outputCmds.add(Command.STOP.code);
	}

	@Override
	public void enterDeclaration(SimpleLangParser.DeclarationContext ctx) {
		String name = ctx.ID().getText();
		FuncContext context = new FuncContext(name);
		
		for (SimpleLangParser.ParameterContext param : ctx.parameters()
				.parameter()) {
			context.params.put(param.getText(), UNKNOWN_OFFSET);
		}

		new ExpressionListener(context).enterExpression(ctx.expression());
		
		functions.put(name, context);
		context.offset = funcCmds.size() + 1;
		funcCmds.addAll(context.cmds);
		printCommands(name, context.cmds);
	}
	
	@Override
	public void exitOutput(SimpleLangParser.OutputContext ctx) {
		outputCmds.addAll(funcCmds);
	}

	private FuncContext anonymous(List<Integer> cmds, Map<String, Integer> params) {
		return new FuncContext(ANONYMOUS_CONTEXT_NAME, params, cmds);
	}
	
	private class FuncContext {
		public final String name;
		public final Map<String, Integer> params;
		public final List<Integer> cmds;
		public int offset;

		public FuncContext(String name) {
			params = new HashMap<>();
			cmds = new ArrayList<>();
			this.name = name;
		}
		
		public FuncContext(String name, Map<String, Integer> params, List<Integer> cmds) {
			this.name = name;
			this.params = params;
			this.cmds = cmds;
		}

		public FuncContext(FuncContext func) {
			this.name = func.name;
			
			this.params = new HashMap<>(func.params.size());
			for (Entry<String, Integer> param : func.params.entrySet()) {
				this.params.put(param.getKey(), param.getValue().intValue());
			}
			
			this.cmds = new ArrayList<>();
			Collections.copy(this.cmds, func.cmds);
		}
	}
}
