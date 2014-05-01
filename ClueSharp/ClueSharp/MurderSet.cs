using System;
using System.Collections.Generic;

namespace ClueSharp
{
  public struct MurderSet
  {
    private readonly Suspect m_suspect;
    private readonly Weapon m_weapon;
    private readonly Room m_room;

    public MurderSet(object card1, object card2, object card3)
      : this((Suspect)card1, (Weapon)card2, (Room)card3)
    {
    }

    public MurderSet(Suspect suspect, Weapon weapon, Room room)
    {
      m_suspect = suspect;
      m_weapon = weapon;
      m_room = room;
    }

    public Suspect Suspect
    {
      get { return m_suspect; }
    }

    public Weapon Weapon
    {
      get { return m_weapon; }
    }

    public Room Room
    {
      get { return m_room; }
    }

    public override string ToString()
    {
      return EnumConversion.Convert(Suspect)
        + " " + EnumConversion.Convert(Weapon)
        + " " + EnumConversion.Convert(Room);
    }

    public static IEnumerable<MurderSet> AllSuggestions 
    {
      get
      {
        var suspects = Enum.GetValues(typeof (Suspect));
        var weapons = Enum.GetValues(typeof(Weapon));
        var rooms = Enum.GetValues(typeof(Room));
        
        foreach (var suspect in suspects)
        {
          foreach (var weapon in weapons)
          {
            foreach (var room in rooms)
            {
              // TODO: i don't like this
              if ((Suspect)suspect == Suspect.None || (Suspect)suspect == Suspect.Count
                || (Weapon)weapon == Weapon.None || (Weapon)weapon == Weapon.Count
                || (Room)room == Room.None || (Room)room == Room.Count)
              { continue; }
                yield return new MurderSet(suspect, weapon, room);
            }
          }
        }
      }
    }
  }
}