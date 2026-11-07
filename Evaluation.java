import java.util.Locale;
import java.util.Random;

/** Seeded evaluation of the original reset distribution and continuous save rule. */
public class Evaluation {
    record Result(int shots, long seed, int saves, int stationarySaves,
                  double normalizedMse, double normalizedMae, double endpointRmse) {
        String json() {
            return String.format(Locale.ROOT,
                "{\"shots\":%d,\"seed\":%d,\"saves\":%d,\"save_rate\":%.12f,"
                + "\"stationary_saves\":%d,\"stationary_save_rate\":%.12f,"
                + "\"normalized_velocity_mse\":%.12g,\"normalized_velocity_rmse\":%.12g,"
                + "\"normalized_velocity_mae\":%.12g,\"endpoint_rmse_world_units\":%.12g}",
                shots, seed, saves, (double)saves/shots, stationarySaves, (double)stationarySaves/shots,
                normalizedMse, Math.sqrt(normalizedMse), normalizedMae, endpointRmse);
        }
    }

    static Result measure(NeuralNetwork nn, int shots, long seed) {
        if (shots <= 0) throw new IllegalArgumentException("shots must be positive");
        World world = new World();
        Random random = new Random(seed);
        int saves = 0, stationary = 0;
        double squaredError = 0, absoluteError = 0, endpointSquaredError = 0;
        double low = (world.pitch.width - world.goal_width) / 2.0;
        double high = low + world.goal_width;
        double centre = world.pitch.width / 2.0;
        double halfReach = world.keeper.saveRange / 2.0;
        for (int i = 0; i < shots; i++) {
            // Same distribution as World.reset(), using a local seed rather than Math.random().
            world.ball.position.x = centre;
            world.ball.position.y = world.startBallY;
            world.ball.velocity.y = world.ball.vy_min + random.nextDouble() * (world.ball.vy_max-world.ball.vy_min);
            double time = world.startBallY / world.ball.velocity.y;
            double target = low + 1 + random.nextDouble() * (high-low-2);
            world.ball.velocity.x = (target-centre) / time;
            world.keeper.position.x = centre;
            world.keeper.velocity.x = 0;
            double prediction = nn.getOutputs(world.getInputs())[0];
            if (!Double.isFinite(prediction)) throw new IllegalStateException("Nonfinite model output");
            double expected = Math.max(-1, Math.min(1, (target-centre)/time/Main.MAX_KEEPER_VELOCITY));
            double end = Math.max(low+halfReach, Math.min(high-halfReach,
                    centre+prediction*Main.MAX_KEEPER_VELOCITY*time));
            if (Math.abs(target-end) <= halfReach) saves++;
            if (Math.abs(target-centre) <= halfReach) stationary++;
            double error = prediction-expected;
            squaredError += error*error;
            absoluteError += Math.abs(error);
            endpointSquaredError += (target-end)*(target-end);
        }
        return new Result(shots, seed, saves, stationary, squaredError/shots,
                absoluteError/shots, Math.sqrt(endpointSquaredError/shots));
    }
}
