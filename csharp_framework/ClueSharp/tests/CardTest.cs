using System;
using System.Linq;
using NUnit.Framework;

namespace ClueSharp.tests
{
  [TestFixture]
  public class CardTest
  {
    [Test]
    public void EqualityTest()
    {
      Assert.AreEqual(new Card(Room.Kitchen), new Card(Room.Kitchen));
    }

    [Test]
    public void TestToString()
    {
      Assert.AreEqual("Wr", new Card(Weapon.MonkeyWrench).ToString());
    }

    [Test]
    public void TestValue()
    {
      Assert.AreEqual(Weapon.MonkeyWrench, new Card(Weapon.MonkeyWrench).Value);
    }

    [Test]
    public void CanOnlyConstructFromACardEnumType()
    {
      Assert.Throws<Exception>(() => new Card("asdf"));
    }

    [Test]
    public void TestAllValues()
    {
      const int total = (int)Suspect.Count + (int)Weapon.Count + (int)Room.Count;
      Assert.AreEqual(total, Card.AllValues.Count());
    }

  }
}
