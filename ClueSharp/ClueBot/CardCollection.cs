using System;
using System.Collections.Generic;
using System.Linq;
using ClueSharp;

namespace ClueBot
{
  internal class CardCollection
  {
    internal CardCollection(IEnumerable<Suspect> suspects, IEnumerable<Weapon> weapons, IEnumerable<Room> rooms)
    {
      Suspects = suspects;
      Weapons = weapons;
      Rooms = rooms;
    }

    internal CardCollection(List<object> cards)
      : this(cards.OfType<Suspect>(),
             cards.OfType<Weapon>(),
             cards.OfType<Room>())
    {
    }

    internal IEnumerable<Suspect> Suspects { get; set; }

    internal IEnumerable<Weapon> Weapons { get; set; }

    internal IEnumerable<Room> Rooms { get; set; }
  }
}