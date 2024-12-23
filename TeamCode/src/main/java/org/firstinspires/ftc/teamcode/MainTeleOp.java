package org.firstinspires.ftc.teamcode;

import android.os.Build;
import androidx.annotation.RequiresApi;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


enum MotorType {
    LEFT_FRONT("left_front_drive"),
    LEFT_BACK("left_back_drive"),
    RIGHT_FRONT("right_front_drive"),
    RIGHT_BACK("right_back_drive"),
    LEFT_SLIDE("left_slide"),
    RIGHT_SLIDE("right_slide"),
    INTAKE("intake_motor");

    private final String name;

    MotorType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}

@TeleOp(name = "Main_TeleOp", group = "Linear OpMode")
public class MainTeleOp extends LinearOpMode {
    private final Map<MotorType, DcMotor> motors = new HashMap<>();

    private Servo intake_rotation = null;

    private int slideOut = 2200;
    private int slideIn = 0;

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void moveRobot(double x, double y, double yaw) {
        // Calculate wheel powers.
        Map<MotorType, Double> powers = new HashMap<MotorType, Double>() {{
            put(MotorType.LEFT_FRONT, y + x + yaw);
            put(MotorType.RIGHT_FRONT, y - x - yaw);
            put(MotorType.LEFT_BACK, y - x + yaw);
            put(MotorType.RIGHT_BACK, y + x - yaw);
        }};

        // Normalize wheel powers to be less than 1.0
        double max = powers
                .values()
                .stream()
                .map(Math::abs)
                .max(Double::compare)
                .orElse(1.0);

        if (max > 1.0)
            powers.keySet().forEach(motorType -> powers.put(motorType, powers.get(motorType) / max));

        // Set motor powers
        double power_module = 0.5f;
        for (MotorType motorType : new MotorType[]{
                MotorType.LEFT_FRONT, MotorType.LEFT_BACK, MotorType.RIGHT_FRONT, MotorType.RIGHT_BACK}) {
            Objects.requireNonNull(
                    motors.get(motorType)).setPower(Objects.requireNonNull(powers.get(motorType)) * power_module);
        }

        // log data
        telemetry.addData("Motor Powers", powers);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void runOpMode() {
        for (MotorType motorType : MotorType.values())
            motors.put(motorType, hardwareMap.get(DcMotor.class, motorType.toString()));

        intake_rotation = hardwareMap.get(Servo.class, "intake_rotation");

        Objects.requireNonNull(motors.get(MotorType.LEFT_FRONT))
                .setDirection(DcMotor.Direction.REVERSE);
        Objects.requireNonNull(motors.get(MotorType.LEFT_BACK))
                .setDirection(DcMotor.Direction.REVERSE);
        Objects.requireNonNull(motors.get(MotorType.RIGHT_FRONT))
                .setDirection(DcMotor.Direction.FORWARD);
        Objects.requireNonNull(motors.get(MotorType.RIGHT_BACK))
                .setDirection(DcMotor.Direction.FORWARD);

        DcMotor leftSlideDrive = Objects.requireNonNull(motors.get(MotorType.LEFT_SLIDE));
        leftSlideDrive.setDirection(DcMotor.Direction.FORWARD);
        leftSlideDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftSlideDrive.setTargetPosition(0);
        leftSlideDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        DcMotor rightSlideDrive = Objects.requireNonNull(motors.get(MotorType.RIGHT_SLIDE));
        rightSlideDrive.setDirection(DcMotor.Direction.REVERSE);
        rightSlideDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightSlideDrive.setTargetPosition(0);
        rightSlideDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        double rotation = 0;
        boolean slide_active;

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            telemetry.addData("Status", "Running");

            moveRobot(gamepad1.left_stick_x, gamepad1.left_stick_y, gamepad1.right_stick_x);
            if (gamepad2.x)
                rotation = Math.min(rotation + 1, 270);
            if (gamepad2.y)
                rotation = Math.max(rotation - 1, 0);

            intake_rotation.setPosition(rotation / 270.0);
            telemetry.addData("Servo_rotation", rotation);

            slide_active = gamepad1.x;

            if (slide_active) {
                leftSlideDrive.setTargetPosition(slideOut);
                rightSlideDrive.setTargetPosition(slideOut);

                leftSlideDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                rightSlideDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                leftSlideDrive.setPower(0.75);
                rightSlideDrive.setPower(0.75);

                telemetry.addData("Status", "Moving to position: " + slideOut);
            } else {
                leftSlideDrive.setTargetPosition(slideIn);
                rightSlideDrive.setTargetPosition(slideIn);

                leftSlideDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                rightSlideDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                leftSlideDrive.setPower(0.5);
                rightSlideDrive.setPower(0.5);

                telemetry.addData("Status", "Moving to position: " + slideIn);
            }

            // Once target is reached, motor will automatically stop.
            if (!leftSlideDrive.isBusy()) {
                leftSlideDrive.setPower(0);
                telemetry.addData("Status", "left reached");
            }

            if (!rightSlideDrive.isBusy()) {
                rightSlideDrive.setPower(0);
                telemetry.addData("Status", "right reached");
            }

            telemetry.addData("Status", "Slide at %7d", leftSlideDrive.getCurrentPosition());
            telemetry.addData("Status", "Slide at %7d", rightSlideDrive.getCurrentPosition());
            telemetry.update();
        }
    }
}
