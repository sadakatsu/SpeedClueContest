using System;
using System.Collections.Generic;
using System.Diagnostics;
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

    internal bool DoesHave(int i, Enum value)
    {
      var changed = false;
      var array = this[value];
      for (int j = 0; j <= m_n; j++)
      {
        if (j == i) { continue; }
        if (array[j])
        {
          changed = true;
        }
        array[j] = false;
      }
      array[i] = true;
      return changed;
    }

    internal bool DoesNotHave(int i, Enum value)
    {
      var ret = this[value][i];
      this[value][i] = false;
      return ret;
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

    internal bool MustHavesAndMightHaves(List<Enum> list, int numberOfCards, int player)
    {
      bool ret = CountHowManyMustHaves(list, numberOfCards, player);
      ret = ret & CountHowManyMightHaves(list, numberOfCards, player);
      return ret;
    }

    private bool CountHowManyMightHaves(List<Enum> list, int numberOfCards, int player)
    {
      var mightHave = list.Where(e => this[e][player]).ToList();
      Debug.Assert(mightHave.Count() >= numberOfCards);
      if (mightHave.Count() == numberOfCards)
      {
        var changed = false;
        foreach (var card in mightHave)
        {
          changed = changed | DoesHave(player, card);
        }
        return changed;
      }
      return false;
    }

    private bool CountHowManyMustHaves(List<Enum> list, int numberOfCards, int player)
    {
      var mustHave = MustHaves(list, player);
      Debug.Assert(mustHave.Count() <= numberOfCards);
      if (mustHave.Count() == numberOfCards)
      {
        var changed = false;
        foreach (var card in list.Where(x => mustHave.IndexOf(x) == -1))
        {
          changed = changed | DoesNotHave(player, card);
        }
        return changed;
      }
      return false;
    }

    public List<Enum> MustHaves(List<Enum> list, int player)
    {
      return list
        .Where(x => !Enumerable
                       .Range(0, m_n + 1)
                       .Where(y => y != player)
                       .Any(y => this[x][y])
        )
        .ToList();
    }
  }
}