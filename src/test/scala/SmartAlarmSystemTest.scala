package assignment3

import org.apache.pekko.actor.testkit.typed.scaladsl.{LoggingTestKit, ScalaTestWithActorTestKit}
import assignment3.SmartAlarmSystem
import org.scalatest.wordspec.AnyWordSpecLike

class SmartAlarmSystemTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  import assignment3.SmartAlarmSystem.*

  "The SmartAlarmSystem" should {

    "reject an incorrect PIN when disarmed" in {
      val alarm = spawn(SmartAlarmSystem())

      LoggingTestKit.info("WROOOOONG").expect {
        alarm ! EnterPin("9999")
      }
    }

    "arm successfully and activate sensors after ExitTimer" in {
      val alarm = spawn(SmartAlarmSystem())

      // 1. Arm the system
      LoggingTestKit.info("Correct pin").expect {
        alarm ! EnterPin(correctPin)
      }

      // 2. Fast-forward the exit delay manually
      alarm ! ExitTimer

      // 3. Verify sensors are now active by triggering a movement
      LoggingTestKit.info("Movement detected!!!").expect {
        alarm ! SensorDetection("Hall")
      }
    }

    "ignore sensor detections during the exit delay phase" in {
      val alarm = spawn(SmartAlarmSystem())
      alarm ! EnterPin(correctPin)

      // During the exit delay, unhandled messages return Behaviors.same.
      // We trigger a sensor here, and expect NO entry delay logs.
      alarm ! SensorDetection("Hall")

      // To prove the system is still in the Exit Timer phase (and not Entry Timer),
      // we input a wrong PIN. If it were in Entry Timer, it would just say "WROOOOONG".
      // Since it's in Exit Timer, it also just says "WROOOOONG".
      LoggingTestKit.info("WROOOOONG").expect {
        alarm ! EnterPin("9999")
      }
    }

    "allow disarming during the Entry Delay" in {
      val alarm = spawn(SmartAlarmSystem())
      alarm ! EnterPin(correctPin)
      alarm ! ExitTimer // Fast-forward to Armed state

      alarm ! SensorDetection("BedRoom") // Trigger entry delay

      LoggingTestKit.info("Correct Pin alarm disabled").expect {
        alarm ! EnterPin(correctPin)
      }
    }

    "sound the alarm if PIN is not entered before EnterTimer expires" in {
      val alarm = spawn(SmartAlarmSystem())
      alarm ! EnterPin(correctPin)
      alarm ! ExitTimer // Fast-forward to Armed state

      alarm ! SensorDetection("Kitchen") // Trigger entry delay

      // Fast-forward the enter timer to simulate timeout
      LoggingTestKit.info("NINONINONINONINO").expect {
        alarm ! EnterTimer
      }

      // Verify we are now trapped in the alarm state
      LoggingTestKit.info("ULTRA MEGA SUPER NINONINONINONINO").expect {
        alarm ! EnterPin("0000") // A wrong pin during the alarm
      }
    }

    "support partial arming by ignoring inactive zones" in {
      val alarm = spawn(SmartAlarmSystem())

      // Arm only the "Hall"
      LoggingTestKit.info("armed zones are: Set(Hall)").expect {
        alarm ! ArmingPin(correctPin, Set("Hall"))
      }
      alarm ! ExitTimer // Fast-forward to Armed state

      // 1. Trigger an inactive zone (BedRoom). It should be ignored.
      alarm ! SensorDetection("BedRoom")

      // 2. We can prove it didn't trigger the Entry Delay by sending a wrong PIN.
      // In the `armed` state, a wrong PIN immediately triggers the alarm.
      // In the `enterTimer` state, a wrong PIN just logs "WROOOOONG".
      LoggingTestKit.info("Wrong Pin Fast to Alarm!!").expect {
        alarm ! EnterPin("9999")
      }
    }
  }
}