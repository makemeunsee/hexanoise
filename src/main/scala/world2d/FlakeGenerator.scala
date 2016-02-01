package world2d

/**
 * Created by mgalilee on 11/01/2016.
 */

import scala.util.Random
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import world2d.HexaGrid.{FlakeGenerationStopper, NeighbourDistribution, Flake}
import scala.async.Async.{async, await}
import scala.util.hashing.MurmurHash3

object FlakeGenerator {
  val sizeMax = 500

  def stringToRandom(string: String): Random = {
    new Random(MurmurHash3.stringHash(string))
  }
}

import FlakeGenerator.stringToRandom

// provide flakes on demand.
// prepare the next flake asynchronously after every flake change.
trait FlakeGenerator {
  def seed0: String

  // compute first flake right away
  private var nextFlake: Future[( Flake[LivingHexagon], String )] = nextFlakeFuture(seed0)

  def flakeSpacing: Int

  implicit def distri: NeighbourDistribution

  implicit def stopper: FlakeGenerationStopper

  private def nextFlakeFuture(seed: String): Future[(Flake[LivingHexagon], String)] = {
    println(s"preparing flake with seed: $seed")
    Future {
      implicit val rnd = stringToRandom(seed)
      val result = HexaGrid.rndFlake(stopper)                   // generate with class stopper
        .foldLeft( Seq.fill(6)(Seq(new LivingHexagon(0,0))) ) { // build each branch from h0
          case (flake, step) =>
            step.nexts                                          // extend step with rotations (=nexts) to provide all branches with a step
              .zip(flake)                                       // bind the steps to the branches
              .map {
                case (neighbour, branch) =>
                  (0 until flakeSpacing)                        // apply scaling here, by stepping multiple times
                    .foldLeft(branch) {
                      case (newBranch, _) =>
                        new LivingHexagon(neighbour(newBranch.head), 0, 0) +: newBranch  // build branch in reverse (root at the bottom of the list) and reverse it later
                    }
              }
        }
        .map(_.reverse) // reverse the branch so the root is head
        .map(_.tail)    // remove the head which is common to all branches
      ( result, seed )
    }
  }

  protected def nextWithSeed(seed: Option[String] = None): Unit = async {
    // load flake
    val ( flake, currentSeed ) = await(nextFlake)
    onFlakeComplete(flake, currentSeed)

    // start computing next flake
    nextFlake = nextFlakeFuture(seed.getOrElse(stringToRandom(currentSeed).nextLong().toString))
  }

  def next(): Unit = nextWithSeed()

  // not much thread safe...
  def forceNext(customSeed: String): Unit = {
    // force a new flake computation if a custom seed is requested
    nextFlake = nextFlakeFuture(customSeed)

    nextWithSeed()
  }

  def onFlakeComplete(flake: Flake[LivingHexagon], flakeSeed: String): Unit
}