using System;
using System.Collections.Generic;
using System.Linq;
using ClueSharp;

namespace CluePaddle
{

  internal class PossessionTable : Dictionary<Enum, bool[]>
  {
    private readonly int m_n;

    internal PossessionTable(int n)
    {
      m_n = n;
      foreach (var value in Card.AllValues)
      {
        var array = new bool[n + 1];
        for (int j = 0; j <= n; j++)
        {
          array[j] = true;
        }
        this[value] = array;
      }

    }

    internal void DoesHave(int i, Enum value)
    {
      var array = new bool[m_n + 1];
      for (int j = 0; j <= m_n; j++)
      {
        array[j] = false;
      }
      array[i] = true;
      this[value] = array;
    }

    internal void DoesNotHave(int i, Enum value)
    {
      this[value][i] = false;
    }

    internal Enum PlayerHasTwoOf(PlayerTriplet triplet)
    {
      var playerDoesNotHave = triplet.m_set.Values
        .Where(x => !OwnedBy(this[x], triplet.m_player))
        .ToList();

      if (playerDoesNotHave.Count != 1)
      {
        return null;
      }

      return playerDoesNotHave.Single();
    }

    public Enum PlayerDoesntHaveTwoOf(PlayerTriplet triplet)
    {
      var playerMightHave = triplet.m_set.Values
        .Where(x => this[x][triplet.m_player])
        .ToList();

      if (playerMightHave.Count != 1)
      {
        return null;
      }

      return playerMightHave.Single();
    }

    public bool NotOwned(Enum card)
    {
      return OwnedBy(this[card], m_n);
    }

    private static bool OwnedBy(bool[] bools, int owner)
    {
      return (bools.Where(x => x).Count() == 1) && bools[owner];
    }

    public bool NotKnown(Enum e)
    {
      return this[e][m_n];
    }
  }
}