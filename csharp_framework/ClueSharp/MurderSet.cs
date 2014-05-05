using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;

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
        return from suspect in Card.AllSuspects
               from weapon in Card.AllWeapons
               from room in Card.AllRooms
               select new MurderSet(suspect, weapon, room);
      }
    }

    public IEnumerable<Enum> Values
    {
      get
      {
        yield return Suspect;
        yield return Weapon;
        yield return Room;
      }
    }
  }
}