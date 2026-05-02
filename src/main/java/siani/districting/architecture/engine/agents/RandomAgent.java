package siani.districting.architecture.engine.agents;

import siani.districting.architecture.engine.Action;
import siani.districting.architecture.engine.Agent;

import java.util.*;

public class RandomAgent implements Agent {
    
    private final Random random = new Random();
    private final int id;

    public RandomAgent(int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public Action choose(Set<Action> actions) {
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        Map<Action, Double> actionValueMap = valueFunction(new HashSet<>(actions));
        return choose(actionValueMap);
    }

    private Action choose(Map<Action, Double> actionValueMap) {
        Set<Action> actionSet = actionValueMap.keySet();
        Action[] actionsArray = actionSet.toArray(new Action[0]);
        return actionsArray[random.nextInt(actionsArray.length)];
    }

    @Override
    public Map<Action, Double> valueFunction(Set<Action> actions) {
        Map<Action, Double> valueMap = new HashMap<>();
        return setRandomValues(valueMap, actions);
    }

    private Map<Action, Double> setRandomValues(Map<Action, Double> valueMap, Set<Action> actionsSet) {
        double[] randomValuesSet = generateRandomValues(actionsSet, random);

        int i = 0;
        for (Action action : actionsSet) {
            valueMap.put(action, randomValuesSet[i++]);
        }
        return valueMap;
    }

    private static double[] generateRandomValues(Set<Action> actionsSet, Random random) {
        double[] valueArray = new double[actionsSet.size()];
        for (int i = 0; i < actionsSet.size(); i++) {
            valueArray[i] = random.nextInt(99) + 1;
        }
        double total = Arrays.stream(valueArray).sum();
        return Arrays.stream(valueArray).map(number -> number / total).toArray();
    }
}
