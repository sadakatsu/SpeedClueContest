using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
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
    public void CanOnlyConstructFromACardEnumType()
    {
      Assert.Throws<Exception>(() => new Card("asdf"));
    }

  }
}
