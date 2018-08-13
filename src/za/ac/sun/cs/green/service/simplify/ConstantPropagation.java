package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropagation extends BasicService {

	private int invocations = 0;

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = simplify(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	public Expression simplify(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Simplification: " + expression);
			invocations++;

			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);

			expression = orderingVisitor.getExpression();

			log.log(Level.FINEST, "After Simplification: " + expression);
			return expression;

		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<Expression, Expression> variable_map;

		public OrderingVisitor() {
			stack = new Stack<Expression>();
			variable_map = new HashMap<Expression, Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) {
			System.out.println("Pushes constant: " + constant);
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			Expression key = variable;
			Expression get = variable_map.get(key);
			if (get != null) {
				//propagation of one constant if it exists in the hash map.
				System.out.println("Pushes variable: " + get);
				stack.push(get);
			} else {
				System.out.println("Pushes variable: " + variable);
				stack.push(variable);
			}

		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			System.out.println("------------------In postVisit: OPERATION.-----------------------");
			Operation.Operator op = operation.getOperator();

			Expression r = stack.pop();
			System.out.println("Pops off r: " + r);
			Expression l = stack.pop();
			System.out.println("Pops off l: " + l);

			if (op == Operation.Operator.EQ) {
				// adds variable and value to the hash map.
				System.out.println("adds variable and constant to hash map: " + l + " == " + r);
				variable_map.put(l, r);

			}

			System.out.println("Pushes a new Operation: \n l: " + l + "\n op: " + op + "\n r: " + r);
			stack.push(new Operation(op, l, r));

			System.out.println("--------------End of postVisit: Operation.---------------------------");
		}

	}


}
