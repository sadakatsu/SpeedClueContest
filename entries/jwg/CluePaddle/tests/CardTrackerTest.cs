using NUnit.Framework;

namespace CluePaddle.tests
{
  [TestFixture]
  class CardTrackerTest
  {
    [Test]
    public void TestSetUpNumberOfCards()
    {
      var tracker = new CardTracker(4);

      Assert.AreEqual(5, tracker.NumberOfCards[0]);
      Assert.AreEqual(5, tracker.NumberOfCards[1]);
      Assert.AreEqual(4, tracker.NumberOfCards[2]);
      Assert.AreEqual(4, tracker.NumberOfCards[3]);
    }

  }
}
