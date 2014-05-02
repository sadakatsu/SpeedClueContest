using System;
using System.Collections.Generic;
using System.Linq;
using ClueSharp;
using NUnit.Framework;

namespace ClueBot.tests
{
  [TestFixture]
  class ParserTest
  {

    [Test]
    public void TestParseOneCard()
    {
      Assert.AreEqual(Room.BilliardsRoom, EnumConversion.ParseOneCard("Bi"));
      Assert.AreEqual(Room.BallRoom, EnumConversion.ParseOneCard("Ba"));

      Assert.AreEqual(Suspect.MrsWhite, EnumConversion.ParseOneCard("Wh"));

      Assert.AreEqual(Weapon.MonkeyWrench, EnumConversion.ParseOneCard("Wr"));

      Assert.Throws<ArgumentException>(() => EnumConversion.ParseOneCard("Jw"));
    }

    [Test]
    public void TestParseCards()
    {
      var collection = Parser.ParseCards(new List<string>
        {
          "Ro",
          "Wr",
          "Sc"
        });
      Assert.AreEqual(1, collection.Suspects.Count());
      Assert.AreEqual(2, collection.Weapons.Count());
      Assert.AreEqual(0, collection.Rooms.Count());

      collection = Parser.ParseCards(new List<string>
        {
          "Ro",
          "Lo",
          "Ba",
          "Ki"
        });
      Assert.AreEqual(0, collection.Suspects.Count());
      Assert.AreEqual(1, collection.Weapons.Count());
      Assert.AreEqual(3, collection.Rooms.Count());
    }

    [Test]
    public void TestParseSetValid()
    {
      var s = new[] {"Sc", "Pi", "Di"};
      var set = Parser.ParseSet(s);
      Assert.AreEqual(Suspect.MissScarlet, set.Suspect);
      Assert.AreEqual(Weapon.LeadPipe, set.Weapon);
      Assert.AreEqual(Room.DiningRoom, set.Room);
    }
  }
}
