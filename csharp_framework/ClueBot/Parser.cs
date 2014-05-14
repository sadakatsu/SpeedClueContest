using System;
using System.Collections.Generic;
using System.Linq;
using ClueSharp;

namespace ClueBot
{
  class Parser
  {

    internal static CardCollection ParseCards(IEnumerable<string> strings)
    {
      return new CardCollection(strings.Select(EnumConversion.ParseOneCard).ToList());
    }

    internal static int ParseInt(string s)
    {
      return Int32.Parse(s);
    }

    public static MurderSet ParseSet(IEnumerable<string> cards)
    {
      var cardList = cards.ToList();
      return new MurderSet(EnumConversion.ParseOneCard(cardList[0]),
                           EnumConversion.ParseOneCard(cardList[1]),
                           EnumConversion.ParseOneCard(cardList[2]));
    }

    public static Card ParseCard(string s)
    {
      var o = EnumConversion.ParseOneCard(s);
      return new Card(o);
    }

    public static int? ParseIntMaybe(string s)
    {
      if (s == "-")
      {
        return null;
      }
      return ParseInt(s);
    }

    public static bool ParseBool(string s)
    {
      if (s == "-")
      {
        return false;
      }
      if (s == "+")
      {
        return true;
      }
      throw new Exception();
    }
  }
}