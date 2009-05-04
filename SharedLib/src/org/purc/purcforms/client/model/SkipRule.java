package org.purc.purcforms.client.model;

import java.io.Serializable;
import java.util.Vector;

import org.purc.purcforms.client.xforms.XformConverter;


/**
 * A definition for skipping or branching rules. 
 * These could for example be enabling or disabling, hiding or showing, maing mandatory or optional 
 * of questions basing on values of others.
 * 
 * @author Daniel Kayiwa
 *
 */
public class SkipRule implements Serializable{
	
	/** The numeric identifier of a rule. This is assigned in code and hence
	 * is not known by the user.
	 */
	private int id = ModelConstants.NULL_ID;
	
	/** A list of conditions (Condition object) to be tested for a rule. 
	 * E.g. If sex is Male. If age is greatern than 4. etc
	 */
	private Vector conditions;
	
	/** The action taken when conditions are true.
	 * Example of actions are Disable, Hide, Show, etc
	 */
	private int action = ModelConstants.ACTION_NONE;
	
	/** A list of question identifiers (int) acted upon when conditions for the rule are true. */
	private Vector actionTargets;
	
	/** The skip rule name. */
	//private String name; (No use found for this as for now)
	
	/** Operator for combining more than one condition. (And, Or) only these two for now. */
	private int conditionsOperator = ModelConstants.CONDITIONS_OPERATOR_NULL;
	
		
	/** Constructs a rule object ready to be initialized. */
	public SkipRule(){

	}
	
	/** Copy constructor. */
	public SkipRule(SkipRule skipRule){
		setId(skipRule.getId());
		setAction(skipRule.getAction());
		//setName(skipRule.getName());
		setConditionsOperator(skipRule.getConditionsOperator());
		copyConditions(skipRule.getConditions());
		copyActionTargets(skipRule.getActionTargets());
	}
	
	/** Construct a Rule object from parameters. 
	 * 
	 * @param ruleId 
	 * @param conditions 
	 * @param action
	 * @param actionTargets
	 */
	public SkipRule(int ruleId, Vector conditions, int action, Vector actionTargets /*, String name*/) {
		setId(ruleId);
		setConditions(conditions);
		setAction(action);
		setActionTargets(actionTargets);
		//setName(name);
	}
	
	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public Vector getActionTargets() {
		return actionTargets;
	}

	public void setActionTargets(Vector actionTargets) {
		this.actionTargets = actionTargets;
	}

	public Vector getConditions() {
		return conditions;
	}

	public void setConditions(Vector conditions) {
		this.conditions = conditions;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public int getConditionsOperator() {
		return conditionsOperator;
	}

	public void setConditionsOperator(int conditionsOperator) {
		this.conditionsOperator = conditionsOperator;
	}
	
	public Condition getConditionAt(int index) {
		if(conditions == null)
			return null;
		return (Condition)conditions.elementAt(index);
	}
	
	public int getConditionCount() {
		if(conditions == null)
			return 0;
		return conditions.size();
	}
	
	public int getActionTargetCount() {
		if(actionTargets == null)
			return 0;
		return actionTargets.size();
	}

	public Integer getActionTargetAt(int index) {
		if(actionTargets == null)
			return null;
		return (Integer)actionTargets.elementAt(index);
	}
	/*public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}*/
	
	public void addActionTarget(int id){
		if(actionTargets == null)
			actionTargets = new Vector();
		actionTargets.add(new Integer(id));
	}
	
	public  boolean containsActionTarget(int id){
		if(actionTargets == null)
			return false;
		for(int i=0; i<actionTargets.size(); i++){
			if(((Integer)actionTargets.elementAt(i)).intValue() == id)
				return true;
		}
		return false;
	}
	
	public void addCondition(Condition condition){
		if(conditions == null)
			conditions = new Vector();
		conditions.add(condition);
	}
	
	public boolean containsCondition(Condition condition){
		if(conditions == null)
			return false;
		return conditions.contains(condition);
	}
	
	public void updateCondition(Condition condition){
		for(int i=0; i<conditions.size(); i++){
			Condition cond = (Condition)conditions.elementAt(i);
			if(cond.getId() == condition.getId()){
				conditions.remove(i);
				conditions.add(condition);
				break;
			}
		}
	}
	
	public void removeCondition(Condition condition){
		conditions.remove(condition);
	}
	
	public void removeQuestion(QuestionDef questionDef){
		for(int index = 0; index < getConditionCount(); index++){
			Condition condition = getConditionAt(index);
			if(condition.getQuestionId() == questionDef.getId()){
				removeCondition(condition);
				index++;
			}
		}
		
		for(int index = 0; index < getActionTargetCount(); index++){
			Integer id = getActionTargetAt(index);
			if(id.intValue() == questionDef.getId()){
				this.actionTargets.remove(id);
				index++;
			}
		}
	}

	/** 
	 * Checks conditions of a rule and executes the corresponding actions
	 * 
	 * @param data
	 */
	public void fire(FormDef formDef){
		boolean trueFound = false, falseFound = false;
		
		for(int i=0; i<getConditions().size(); i++){
			Condition condition = (Condition)this.getConditions().elementAt(i);
			if(condition.isTrue(formDef,false))
				trueFound = true;
			else
				falseFound = true;
		}
		
		if(getConditions().size() == 1 || getConditionsOperator() == ModelConstants.CONDITIONS_OPERATOR_AND)
			ExecuteAction(formDef,!falseFound);
		else if(getConditionsOperator() == ModelConstants.CONDITIONS_OPERATOR_OR)
			ExecuteAction(formDef,trueFound);
		//else do nothing
	}
	
	/** Executes the action of a rule for its conditition's true or false value. */
	public void ExecuteAction(FormDef formDef,boolean conditionTrue){
		Vector qtns = this.getActionTargets();
		for(int i=0; i<qtns.size(); i++)
			ExecuteAction(formDef.getQuestion(Integer.parseInt(qtns.elementAt(i).toString())),conditionTrue);
	}
	
	/** Executes the rule action on the supplied question. */
	public void ExecuteAction(QuestionDef qtn,boolean conditionTrue){
		qtn.setVisible(true);
		qtn.setEnabled(true);
		qtn.setRequired(false);
		
		if((action & ModelConstants.ACTION_ENABLE) != 0)
			qtn.setEnabled(conditionTrue);
		else if((action & ModelConstants.ACTION_DISABLE) != 0)
			qtn.setEnabled(!conditionTrue);
		else if((action & ModelConstants.ACTION_SHOW) != 0)
			qtn.setVisible(conditionTrue);
		else if((action & ModelConstants.ACTION_HIDE) != 0)
			qtn.setVisible(!conditionTrue);
		
		if((action & ModelConstants.ACTION_MAKE_MANDATORY) != 0)
			qtn.setRequired(conditionTrue);
	}
	
	/*public String toString() {
		return getName();
	}*/
	
	private void copyConditions(Vector conditions){
		this.conditions = new Vector();
		for(int i=0; i<conditions.size(); i++)
			this.conditions.addElement(new Condition((Condition)conditions.elementAt(i)));
	}
	
	private void copyActionTargets(Vector actionTargets){
		this.actionTargets = new Vector();
		for(int i=0; i<actionTargets.size(); i++)
			this.actionTargets.addElement(new Integer(((Integer)actionTargets.elementAt(i)).intValue()));
	}
	
	public void updateDoc(FormDef formDef){
		XformConverter.fromSkipRule2Xform(this,formDef);
		/*for(int i=0; i<actionTargets.size(); i++){
			int id = ((Integer)actionTargets.elementAt(i)).intValue();
			QuestionDef questionDef = formDef.getQuestion(id);
			Element node = questionDef.getBindNode();
			if(node == null)
				node = questionDef.getControlNode();
			node.setAttribute(EpihandyXform.ATTRIBUTE_NAME_RELEVANT, "");
		}*/
	}
	
	public void refresh(FormDef dstFormDef, FormDef srcFormDef){
		SkipRule skipRule = null;
		
		for(int index = 0; index < this.getConditionCount(); index++){
			Condition condition = getConditionAt(index);
			QuestionDef qtn = srcFormDef.getQuestion(condition.getQuestionId());
			if(qtn == null)
				continue;
			QuestionDef questionDef = dstFormDef.getQuestion(qtn.getVariableName());
			if(questionDef == null)
				continue;
			if(skipRule == null)
				skipRule = new SkipRule();
			condition.setQuestionId(questionDef.getId());
			skipRule.addCondition(new Condition(condition));
		}
		
		if(skipRule == null)
			return; //No matching condition found.
		
		for(int index = 0; index < this.getActionTargetCount(); index++){
			Integer actionTarget = getActionTargetAt(index);
			QuestionDef qtn = srcFormDef.getQuestion(actionTarget);
			if(qtn == null)
				continue;
			QuestionDef questionDef = dstFormDef.getQuestion(qtn.getVariableName());
			if(questionDef == null)
				continue;
			skipRule.addActionTarget(questionDef.getId());
		}
		
		if(skipRule.getActionTargetCount() > 0)
			dstFormDef.addSkipRule(skipRule);
	}
	
	public void updateConditionValue(String origValue, String newValue){
		for(int index = 0; index < this.getConditionCount(); index++)
			getConditionAt(index).updateValue(origValue, newValue);
	}
}
 